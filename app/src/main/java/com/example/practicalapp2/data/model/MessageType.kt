package com.example.practicalapp2.data.model

enum class MessageType(val displayName: String) {
    TEXT("Text"),
    IMAGE("Photo"),
    VIDEO("Video"),
    AUDIO("Audio"),
    VOICE_NOTE("Voice Note"),
    VIDEO_NOTE("Video Note"),
    DOCUMENT("Document"),
    STICKER("Sticker"),
    GIF("GIF"),
    DELETED("Deleted");

    companion object {
        fun fromString(type: String?): MessageType {
            return try {
                if (type == null) TEXT else valueOf(type)
            } catch (e: Exception) {
                TEXT
            }
        }
    }
}
