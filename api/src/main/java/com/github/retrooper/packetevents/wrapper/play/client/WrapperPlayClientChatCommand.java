package com.github.retrooper.packetevents.wrapper.play.client;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.chat.LastSeenMessages;
import com.github.retrooper.packetevents.protocol.chat.SignedCommandArgument;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.crypto.MessageSignData;
import com.github.retrooper.packetevents.util.crypto.SaltSignature;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public class WrapperPlayClientChatCommand extends PacketWrapper<WrapperPlayClientChatCommand> {
    private String command;
    private MessageSignData messageSignData;
    private List<SignedCommandArgument> signedArguments;
    private @Nullable LastSeenMessages.Update lastSeenMessages;
    private @Nullable LastSeenMessages.LegacyUpdate legacyLastSeenMessages;

    public WrapperPlayClientChatCommand(PacketReceiveEvent event) {
        super(event);
    }

    public WrapperPlayClientChatCommand(String command, MessageSignData messageSignData, List<SignedCommandArgument> signedArguments, @Nullable LastSeenMessages.LegacyUpdate lastSeenMessages) {
        super(PacketType.Play.Client.CHAT_COMMAND);
        this.command = command;
        this.messageSignData = messageSignData;
        this.signedArguments = signedArguments;
        this.legacyLastSeenMessages = lastSeenMessages;
    }

    public WrapperPlayClientChatCommand(String command, MessageSignData messageSignData, List<SignedCommandArgument> signedArguments, @Nullable LastSeenMessages.Update lastSeenMessages) {
        super(PacketType.Play.Client.CHAT_COMMAND);
        this.command = command;
        this.messageSignData = messageSignData;
        this.signedArguments = signedArguments;
        this.lastSeenMessages = lastSeenMessages;
    }

    @Override
    public void read() {
        int maxMessageLength = serverVersion.isNewerThanOrEquals(ServerVersion.V_1_11) ? 256 : 100;
        this.command = readString(maxMessageLength);
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            Instant timestamp = readTimestamp();
            long salt = readLong();
            this.messageSignData = new MessageSignData(new SaltSignature(salt, new byte[0]), timestamp);
            this.signedArguments = readSignedCommandArguments();

            if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19_3)) {
                this.lastSeenMessages = readLastSeenMessagesUpdate();
            } else if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19_1)) {
                boolean signedPreview = readBoolean();
                this.messageSignData.setSignedPreview(signedPreview);
                this.legacyLastSeenMessages = readLegacyLastSeenMessagesUpdate();
            }
        }
    }

    @Override
    public void write() {
        writeString(command);
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19)) {
            writeTimestamp(messageSignData.getTimestamp());
            writeLong(messageSignData.getSaltSignature().getSalt());
            writeSignedCommandArguments(signedArguments);

            if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19_3) && lastSeenMessages != null) {
                writeLastSeenMessagesUpdate(lastSeenMessages);
            } else if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_19_1) && serverVersion.isOlderThanOrEquals(ServerVersion.V_1_19_2) && legacyLastSeenMessages != null) {
                writeBoolean(messageSignData.isSignedPreview());
                writeLegacyLastSeenMessagesUpdate(legacyLastSeenMessages);
            }
        }
    }

    @Override
    public void copy(WrapperPlayClientChatCommand wrapper) {
        this.command = wrapper.command;
        this.signedArguments = wrapper.signedArguments;
        this.lastSeenMessages = wrapper.lastSeenMessages;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public MessageSignData getMessageSignData() {
        return messageSignData;
    }

    public void setMessageSignData(MessageSignData messageSignData) {
        this.messageSignData = messageSignData;
    }

    public List<SignedCommandArgument> getSignedArguments() {
        return signedArguments;
    }

    public void setSignedArguments(List<SignedCommandArgument> signedArguments) {
        this.signedArguments = signedArguments;
    }

    public LastSeenMessages.@Nullable Update getLastSeenMessages() {
        return lastSeenMessages;
    }

    public void setLastSeenMessages(LastSeenMessages.@Nullable Update lastSeenMessages) {
        this.lastSeenMessages = lastSeenMessages;
    }

    public @Nullable LastSeenMessages.LegacyUpdate getLegacyLastSeenMessages() {
        return legacyLastSeenMessages;
    }

    public void setLegacyLastSeenMessages(@Nullable LastSeenMessages.LegacyUpdate lastSeenMessages) {
        this.legacyLastSeenMessages = lastSeenMessages;
    }
}