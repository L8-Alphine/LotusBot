package com.lotusacademy.Handlers;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class UserHandler {

    public String getName(Member member){
        return member.getUser().getName();
    }

    public String getAvatar(Member member){
        return member.getUser().getAvatarUrl();
    }

    public List<Role> getRoles(Member member){
        return member.getRoles();
    }

    public boolean hasRole(Member member, Role role){
        if (getRoles(member).contains(role)){
            return  true;
        }
        return false;
    }

}
