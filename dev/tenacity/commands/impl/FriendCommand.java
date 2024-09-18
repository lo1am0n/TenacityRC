package dev.tenacity.commands.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.tenacity.Tenacity;
import dev.tenacity.commands.Command;
import dev.tenacity.utils.player.ChatUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FriendCommand extends Command {

    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
    private static final File file = new File(Tenacity.DIRECTORY, "Friends.json");
    public static final List<String> friends = new ArrayList<>();

    public FriendCommand() {
        super("friend", "Manage friends", ".f [add/remove] [username]", "f");
        load();
    }

    @Override
    public void execute(String[] args) {
        boolean usage = false;
        if (args.length == 0) {
            ChatUtil.print("Friend list (§d" + friends.size() + "§7):");
            if (friends.isEmpty()) {
                ChatUtil.print(false, "§7- You do not have any friends! :(");
            } else {
                friends.forEach(f -> ChatUtil.print(false, "§7- " + f));
            }
        } else if (args.length >= 2) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            switch (args[0].toLowerCase()) {
                case "add":
                    if (isFriend(name)) {
                        ChatUtil.print("That player is already in your friends list!");
                    } else {
                        friends.add(name);
                        ChatUtil.print("Added §d" + name + " §7to your friends list!");
                    }
                    save();
                    break;
                case "remove":
                    if (isFriend(name)) {
                        friends.removeIf(f -> f.equalsIgnoreCase(name));
                        ChatUtil.print("Removed §d" + name + " §7from your friends list.");
                    } else {
                        ChatUtil.print("That player is not in your friends list!");
                    }
                    save();
                    break;
                default:
                    usage = true;
                    break;
            }
        } else {
            usage = true;
        }
        if (usage) {
            ChatUtil.print("Usage: " + getUsage());
        }
    }

    private void load() {

    }

    public static boolean save() {
        return true;
    }

    private static String serialize() {
        return "";
    }

    public static boolean isFriend(String name) {
        return false;
    }

}