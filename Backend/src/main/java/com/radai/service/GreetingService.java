package com.radai.service;

import java.time.LocalTime;

/**
 * Handles time-based greetings for the chatbot.
 * Determines greeting based on current time of day.
 */
public class GreetingService {

    /**
     * Generate a time-based greeting
     * 05:00-11:59 → "Good morning"
     * 12:00-17:59 → "Good afternoon"
     * 18:00-21:59 → "Good evening"
     * 22:00-04:59 → "Good night"
     */
    public static String generateTimeBasedGreeting() {
        LocalTime now = LocalTime.now();
        
        if (now.isAfter(LocalTime.of(5, 0)) && now.isBefore(LocalTime.of(12, 0))) {
            return "Good morning! 🌅 I hope you're having a good start to your day. How are you feeling right now?";
        } else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return "Good afternoon! ☀️ How's your day going so far? What's on your mind?";
        } else if (now.isAfter(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(22, 0))) {
            return "Good evening! 🌆 Taking a moment to check in with yourself? I'm here to listen.";
        } else {
            return "Good night! 🌙 I'm glad you're reaching out. What's been on your mind today?";
        }
    }

    /**
     * Generate greeting for a specific locale
     */
    public static String generateTimeBasedGreeting(String language) {
        if (language == null) language = "en";
        
        if (language.toLowerCase().startsWith("ms")) {
            return generateMalayGreeting();
        }
        
        return generateTimeBasedGreeting();
    }

    private static String generateMalayGreeting() {
        LocalTime now = LocalTime.now();
        
        if (now.isAfter(LocalTime.of(5, 0)) && now.isBefore(LocalTime.of(12, 0))) {
            return "Selamat pagi! 🌅 Saya berharap hari anda bermula dengan baik. Apa yang anda rasakan sekarang?";
        } else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return "Selamat petang! ☀️ Bagaimanakah hari anda sehingga sekarang? Apa yang ada di fikiran anda?";
        } else if (now.isAfter(LocalTime.of(18, 0)) && now.isBefore(LocalTime.of(22, 0))) {
            return "Selamat malam! 🌆 Mengambil masa untuk memeriksakan diri anda? Saya di sini untuk mendengarkan.";
        } else {
            return "Selamat malam! 🌙 Saya gembira anda menghubungi kami. Apa yang ada di benak anda?";
        }
    }

    public static LocalTime getCurrentTime() {
        return LocalTime.now();
    }
}

