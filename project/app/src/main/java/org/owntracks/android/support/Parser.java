package org.owntracks.android.support;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageEncrypted;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Parser {
    private final ObjectMapper defaultMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private final ObjectMapper arrayCompatMapper = new ObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final EncryptionProvider encryptionProvider;

    @Inject
    public Parser(EncryptionProvider encryptionProvider) {
        this.encryptionProvider = encryptionProvider;
    }

    public String toUnencryptedJsonPretty(@NonNull MessageBase message) throws IOException {
        return defaultMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message).replaceAll("\\r\\n", "\n");
    }

    public String toUnencryptedJson(@NonNull MessageBase message) throws IOException {
        return defaultMapper.writeValueAsString(message);
    }

    public byte[] toUnencryptedJsonBytes(@NonNull MessageBase message) throws IOException {
        return defaultMapper.writeValueAsBytes(message);
    }

    public String toJson(@NonNull MessageBase message) throws IOException {
        return encryptString(toUnencryptedJson(message));
    }

    public byte[] toJsonBytes(@NonNull MessageBase message) throws IOException {
        return encryptBytes(toUnencryptedJsonBytes(message));
    }

    public MessageBase fromJson(@NonNull String input) throws IOException, EncryptionException {
        return decrypt(defaultMapper.readValue(input, MessageBase.class));
    }


    public MessageBase fromUnencryptedJson(@NonNull byte[] input) throws IOException {
        return defaultMapper.readValue(input, MessageBase.class);
    }

    public MessageBase fromJson(@NonNull byte[] input) throws IOException, EncryptionException {
        return decrypt(fromUnencryptedJson(input));
    }

    // Accepts 1) [{plain},{plain},...], 2) {plain}, 3) {encrypted, data:[{plain}, {plain}, ...]} as input stream
    public MessageBase[] fromJson(@NonNull InputStream input) throws IOException, EncryptionException {
        return decrypt(arrayCompatMapper.readValue(input, MessageBase[].class));
    }

    private MessageBase[] decrypt(MessageBase[] a) throws IOException, EncryptionException {
        // Recorder compatiblity, encrypted messages with data array
        if (a == null)
            throw new IOException("null array");

        if (a.length == 1 && a[0] instanceof MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(encryptionProvider.decrypt(((MessageEncrypted) a[0]).getData()), MessageBase[].class);
        } else { // single message wrapped in array by mapper or array of messages
            return a;
        }
    }

    private MessageBase decrypt(MessageBase m) throws IOException, EncryptionException {
        if (m instanceof MessageEncrypted) {
            if (encryptionProvider == null || !encryptionProvider.isPayloadEncryptionEnabled())
                throw new EncryptionException("received encrypted message but payload encryption is not enabled");
            return defaultMapper.readValue(encryptionProvider.decrypt(((MessageEncrypted) m).getData()), MessageBase.class);
        }
        return m;
    }


    private String encryptString(@NonNull String input) throws IOException {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled()) {
            MessageEncrypted m = new MessageEncrypted();
            m.setData(encryptionProvider.encrypt(input));
            return defaultMapper.writeValueAsString(m);
        }
        return input;
    }

    private byte[] encryptBytes(@NonNull byte[] input) throws IOException {
        if (encryptionProvider != null && encryptionProvider.isPayloadEncryptionEnabled()) {
            MessageEncrypted m = new MessageEncrypted();
            m.setData(encryptionProvider.encrypt(input));
            return defaultMapper.writeValueAsBytes(m);
        }
        return input;
    }


    public static class EncryptionException extends Exception {
        EncryptionException(String s) {
            super(s);
        }
    }
}
