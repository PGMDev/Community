package dev.pgm.community.friends;

// Status code returned when adding a friend
public enum FriendRequestStatus {
  ACCEPTED_EXISTING, // The target had already sent a friend request, so request was auto accepted
  PENDING, // Target and sender have no prior requests
  ALREADY_FRIENDS, // The sender and target are already friends
  ALREADY_REQUESTED, // The sender has already sent a friend request
  COOLDOWN, // The sender was recently rejected by the target and must wait
  BLOCKED // The target has toggled off incoming friend requests
}
