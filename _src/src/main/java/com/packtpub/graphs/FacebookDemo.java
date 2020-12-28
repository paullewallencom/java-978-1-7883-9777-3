/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.packtpub.graphs;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author erikc_000
 */
public class FacebookDemo {
    public static void main(String[] args){
        
        final Properties properties = new Properties();
        //Token intentionally out of shipped code. Make this file.
        try(InputStream in = Files.newInputStream(Paths.get(System.getProperty("user.home"), "PacktFacebookDemo.properties"))){
            properties.load(in);
            if(!properties.containsKey("facebookAccessToken")){
                throw new IOException("No key present, get from graph api tool");
            }
        } catch (IOException ex) {
            System.out.println("Unable to read facebook API key");
            System.exit(1);
        }
        final String accessToken = properties.getProperty("facebookAccessToken");
        FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.VERSION_2_9);
        User user = facebookClient.fetchObject("me", User.class);
        
        System.out.println("Name is " + user.getName());
        
        Connection<User> myFriends = facebookClient.fetchConnection("me/friends", User.class);
        System.out.println("Count of my friends using this application: " + myFriends.getData().size());
        myFriends.forEach(list -> show(facebookClient, list));
    }

    private static void show(FacebookClient facebookClient23, List<User> list) {
        System.out.println("List of friends for this application");
        list.forEach(user -> System.out.println(String.format(" Someone whose name is %d characters", user.getName().length())));
    }
}
