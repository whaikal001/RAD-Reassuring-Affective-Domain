import {
  CameraControls,
  ContactShadows,
  Environment,
  Text,
} from "@react-three/drei";
import { Suspense, useEffect, useRef, useState } from "react";
import { useChat } from "../hooks/useChat";
import { Avatar } from "./Avatar";

const Dots = (props) => {
  const { loading } = useChat();
  const [loadingText, setLoadingText] = useState("");
  useEffect(() => {
    if (loading) {
      const interval = setInterval(() => {
        setLoadingText((loadingText) => {
          if (loadingText.length > 2) {
            return ".";
          }
          return loadingText + ".";
        });
      }, 800);
      return () => clearInterval(interval);
    } else {
      setLoadingText("");
    }
  }, [loading]);
  if (!loading) return null;
  return (
    <group {...props}>
      <Text fontSize={0.14} anchorX={"left"} anchorY={"bottom"}>
        {loadingText}
        <meshBasicMaterial attach="material" color="black" />
      </Text>
    </group>
  );
};

export const Experience = () => {
  const cameraControls = useRef();
  const { cameraZoomed } = useChat();

  useEffect(() => {
    // Position camera to see head and shoulders
    // Camera position: [x, y, z] - stepped back to see full head
    // Target: [x, y, z] - looking at face level for eye contact
    cameraControls.current.setLookAt(0, 1.2, 2.5, 0, 1.2, 0);
  }, []);

  useEffect(() => {
    if (cameraZoomed) {
      // Zoomed in: close-up of face for detailed expression
      cameraControls.current.setLookAt(0, 1.2, 1.2, 0, 1.2, 0, true);
    } else {
      // Default: head and shoulders framed nicely
      cameraControls.current.setLookAt(0, 1.2, 2.5, 0, 1.2, 0, true);
    }
  }, [cameraZoomed]);
  return (
    <>
      <CameraControls ref={cameraControls} />
      <Environment preset="sunset" />
      {/* Wrapping Dots into Suspense to prevent Blink when Troika/Font is loaded */}
      <Suspense>
          <Dots position-y={2.0} position-x={-0.02} />
      </Suspense>
      <Avatar />
      <ContactShadows opacity={0.8} scale={1.2} position={[0, -0.05, 0]} />
    </>
  );
};
