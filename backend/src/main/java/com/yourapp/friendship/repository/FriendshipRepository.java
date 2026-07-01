package com.yourapp.friendship.repository;

import com.yourapp.friendship.entity.Friendship;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Check if a friendship row already exists between two users (either direction) */
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester.id = :u1 AND f.addressee.id = :u2) OR " +
           "(f.requester.id = :u2 AND f.addressee.id = :u1)")
    Optional<Friendship> findBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    /** All accepted friends of a user */
    @Query("SELECT f FROM Friendship f WHERE " +
           "f.status = 'ACCEPTED' AND (f.requester.id = :userId OR f.addressee.id = :userId)")
    List<Friendship> findAcceptedFriends(@Param("userId") Long userId);

    /** Pending requests where current user is the addressee */
    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingForUser(@Param("userId") Long userId);

    /** Pending requests sent by current user */
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findSentByUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.addressee.id ELSE f.requester.id END " +
           "FROM Friendship f WHERE f.status = 'ACCEPTED' " +
           "AND (f.requester.id = :userId OR f.addressee.id = :userId)")
    List<Long> findAcceptedFriendUserIds(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.addressee.id ELSE f.requester.id END " +
           "FROM Friendship f WHERE f.requester.id = :userId OR f.addressee.id = :userId")
    List<Long> findRelatedUserIds(@Param("userId") Long userId);
}
