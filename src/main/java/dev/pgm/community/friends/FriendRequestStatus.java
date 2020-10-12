package dev.pgm.community.friends;

// Status code returned when adding a friend
public enum FriendRequestStatus {
  ACCEPTED_EXISTING, // The target had already sent a friend request, so request was auto accepted
  PENDING, // Target and sender have no prior requests
  EXISTING; // The sender has already sent a friend request
}
