package org.example.space_invaders_online.game.client;

public record Request(RequestType requestType, String args, int ownerID) {
}
