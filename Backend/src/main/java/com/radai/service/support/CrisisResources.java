package com.radai.service.support;

import java.util.List;
import java.util.Locale;

public final class CrisisResources {

    /** Government 24/7 psychosocial support line (also WhatsApp 019-261 5999). */
    public static final String TALIAN_KASIH = "15999";
    /** Befrienders Kuala Lumpur, 24/7 emotional support. */
    public static final String BEFRIENDERS_KL = "03-7627 2929";
    /** Malaysian Mental Health Association. */
    public static final String MMHA = "03-2780 6803";
    /** National emergency number. */
    public static final String EMERGENCY = "999";

    private CrisisResources() {
    }

    private static boolean isMalay(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("ms");
    }

    /** The primary helpline to surface first (env-overridable, defaults to Talian Kasih). */
    public static String primaryHelpline() {
        String override = System.getenv("CRISIS_HELPLINE");
        return (override == null || override.isBlank()) ? TALIAN_KASIH : override.trim();
    }

    /** A short, prominent, always-available help line for headers / buttons. */
    public static String helpBanner(String language) {
        if (isMalay(language)) {
            return "Jika anda dalam bahaya segera, hubungi 999. Untuk sokongan, hubungi Talian Kasih "
                + primaryHelpline() + " (24 jam).";
        }
        return "If you're in immediate danger, call 999. For support, call Talian Kasih "
            + primaryHelpline() + " (24/7).";
    }

    /** The full, localized list of crisis resources to show in a crisis response. */
    public static List<String> forLanguage(String language) {
        if (isMalay(language)) {
            return List.of(
                "Jika anda dalam bahaya segera, hubungi 999 sekarang.",
                "Talian Kasih (24 jam): " + primaryHelpline() + " — sokongan psikososial.",
                "Befrienders KL (24 jam): " + BEFRIENDERS_KL + " — sokongan emosi & pendengaran.",
                "Persatuan Kesihatan Mental Malaysia (MMHA): " + MMHA + ".",
                "Anda tidak keseorangan. Jika boleh, hubungi seseorang yang dipercayai untuk berada bersama anda.");
        }
        return List.of(
            "If you're in immediate danger, call 999 now.",
            "Talian Kasih (24/7): " + primaryHelpline() + " — psychosocial support.",
            "Befrienders KL (24/7): " + BEFRIENDERS_KL + " — emotional support & a listening ear.",
            "Malaysian Mental Health Association (MMHA): " + MMHA + ".",
            "You are not alone. If you can, reach out to someone you trust to be with you.");
    }

    /** Up-front framing that the assistant is not a therapist or a crisis service. */
    public static String disclaimer(String language) {
        if (isMalay(language)) {
            return "Saya rakan sokongan digital, bukan ahli terapi atau perkhidmatan kecemasan. "
                + "Untuk bantuan profesional atau kecemasan, sila gunakan sumber di atas.";
        }
        return "I'm a digital support companion, not a therapist or an emergency service. "
            + "For professional or emergency help, please use the resources above.";
    }
}
