package com.stardust.banksys;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class DummyConsoleSender implements ConsoleCommandSender {

    private final PermissibleBase perm;

    public DummyConsoleSender() {
        this.perm = new PermissibleBase(this);
    }

    @Override
    public void sendMessage(String s) {

    }

    @Override
    public void sendMessage(String[] strings) {

    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {

    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String... strings) {

    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public String getName() {
        return "PvpCommandSender";
    }

    @NotNull
    @Override
    public Spigot spigot() {
        return null;
    }

    @Override
    public @NotNull Component name() {
        return null;
    }

    @Override
    public boolean isPermissionSet(String s) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return true;
    }

    @Override
    public boolean hasPermission(String s) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return true;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
        return perm.addAttachment(plugin, s, b);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
        return perm.addAttachment(plugin, s, b, i);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        return perm.addAttachment(plugin, i);
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {
        perm.removeAttachment(permissionAttachment);
    }

    @Override
    public void recalculatePermissions() {
        perm.recalculatePermissions();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return perm.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean b) {
        return;
    }

    @Override
    public boolean isConversing() {
        return false;
    }

    @Override
    public void acceptConversationInput(String s) {

    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return false;
    }

    @Override
    public void abandonConversation(Conversation conversation) {

    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent conversationAbandonedEvent) {

    }

    @Override
    public void sendRawMessage(String s) {

    }

    @Override
    public void sendRawMessage(@Nullable UUID uuid, @NotNull String s) {

    }
}

