#!/usr/bin/env python3
"""
XTTS v2 Text-to-Speech Generator
Generates speech using Coqui XTTS v2 (supports 28+ languages including Malay)
Usage: python tts_xtts.py "<text>" "<output_wav_path>" "<language_code>"
"""

import sys
import os
import torch
from TTS.api import TTS

def generate_speech(text, output_path, language="en"):
    """
    Generate speech using XTTS v2
    
    Args:
        text: Text to convert to speech
        output_path: Path to save WAV file
        language: Language code (e.g., 'en', 'ms')
    """
    try:
        # Initialize TTS model
        print(f"Initializing XTTS v2 model for language: {language}")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        tts = TTS(model_name="tts_models/multilingual/multi-dataset/xtts_v2", 
                  gpu=device == "cuda", 
                  progress_bar=True)
        
        # Generate speech
        print(f"Generating speech for: {text[:50]}...")
        tts.tts_to_file(text=text, 
                       speaker_wav="default_speaker.wav",  # Uses default speaker
                       language=language,
                       file_path=output_path)
        
        print(f"Speech saved to: {output_path}")
        sys.exit(0)
    except Exception as e:
        print(f"Error generating speech: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python tts_xtts.py '<text>' '<output_path>' '<language>'", file=sys.stderr)
        sys.exit(1)
    
    text = sys.argv[1]
    output_path = sys.argv[2]
    language = sys.argv[3]
    
    generate_speech(text, output_path, language)
