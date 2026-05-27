import { useEffect, useRef, useCallback } from 'react';

/**
 * useAngularBridge Hook
 * Allows the r3f React app to receive commands from Angular parent
 * Place this hook in your Experience or Avatar component
 * 
 * Usage:
 * const { playAnimation, setExpression, playAudio } = useAngularBridge();
 */
export const useAngularBridge = () => {
  const commandsRef = useRef({});

  useEffect(() => {
    const handleMessage = (event) => {
      // Security: only accept messages from localhost
      if (!event.origin.startsWith('http://localhost')) {
        return;
      }

      const { type, data, animation, expression, audio, lipsync, zoomed } = event.data;

      switch (type) {
        case 'PING':
          // Angular is checking if iframe is ready
          window.parent.postMessage({ type: 'PONG' }, '*');
          break;

        case 'PLAY_ANIMATION':
          // Play animation
          if (commandsRef.current.playAnimation) {
            commandsRef.current.playAnimation(animation);
          }
          break;

        case 'SET_EXPRESSION':
          // Set facial expression
          if (commandsRef.current.setExpression) {
            commandsRef.current.setExpression(expression);
          }
          break;

        case 'PLAY_AUDIO':
          // Play audio with lip-sync
          if (commandsRef.current.playAudio) {
            commandsRef.current.playAudio({
              audio,
              lipsync,
              animation
            });
          }
          break;

        case 'FULL_RESPONSE':
          // Full response with animation + expression + audio
          if (commandsRef.current.playFullResponse) {
            commandsRef.current.playFullResponse(data);
          }
          break;

        case 'SET_CAMERA_ZOOM':
          // Zoom camera
          if (commandsRef.current.setCameraZoom) {
            commandsRef.current.setCameraZoom(zoomed);
          }
          break;

        case 'RESET':
          // Reset to idle state
          if (commandsRef.current.reset) {
            commandsRef.current.reset();
          }
          break;

        case 'USER_MESSAGE':
          // User sent a message - show listening/thinking expression
          if (commandsRef.current.onUserMessage) {
            commandsRef.current.onUserMessage(data);
          }
          break;

        default:
          console.log('Unknown message type:', type);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  /**
   * Register command handlers from your Avatar/Experience component
   */
  const registerHandlers = (handlers) => {
    commandsRef.current = handlers;
  };

  return { registerHandlers };
};
