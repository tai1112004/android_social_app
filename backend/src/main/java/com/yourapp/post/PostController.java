package com.yourapp.post;

import com.yourapp.friendship.repository.FriendshipRepository;
import com.yourapp.notification.NotificationService;
import com.yourapp.post.dto.CommentDto;
import com.yourapp.post.dto.PostDto;
import com.yourapp.post.entity.Post;
import com.yourapp.post.entity.PostComment;
import com.yourapp.post.entity.PostCommentReaction;
import com.yourapp.post.repository.PostCommentReactionRepository;
import com.yourapp.post.repository.PostCommentRepository;
import com.yourapp.post.repository.PostRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostCommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    public PostController(PostRepository postRepository, PostCommentRepository commentRepository,
                          PostCommentReactionRepository commentReactionRepository,
                          UserRepository userRepository, FriendshipRepository friendshipRepository,
                          NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Map<String, String> body) {
        User current = currentUser();
        String content = body.get("content");
        String mediaUrl = body.get("mediaUrl");
        if ((content == null || content.trim().isEmpty()) && (mediaUrl == null || mediaUrl.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ná»™i dung hoáº·c media lÃ  báº¯t buá»™c");
        }
        Post post = new Post();
        post.setUser(current);
        post.setContent(content != null ? content.trim() : null);
        post.setMediaUrl(mediaUrl);
        Post saved = postRepository.save(post);
        List<Long> friendIds = friendshipRepository.findAcceptedFriendUserIds(current.getId());
        notificationService.sendToUsers(friendIds, "NEW_POST", "Bai viet moi",
                displayName(current) + " vua dang mot bai viet moi", current,
                notificationService.data("postId", saved.getId(), "mediaUrl", saved.getMediaUrl(), "content", saved.getContent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Da dang bai", toDto(saved, current.getId())));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFeed() {
        User current = currentUser();
        List<Long> friendIds = friendshipRepository.findAcceptedFriendUserIds(current.getId());
        List<Long> authorIds = new ArrayList<>(friendIds);
        authorIds.add(current.getId());
        List<Post> posts = postRepository.findByUserIdIn(authorIds);
        List<PostDto> dtos = posts.stream().map(p -> toDto(p, current.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(ok("OK", dtos));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        User current = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài viết không tồn tại"));
        List<Long> likes = post.getLikedByUserIds();
        if (likes.contains(current.getId())) {
            likes.remove(current.getId());
        } else {
            likes.add(current.getId());
            if (!post.getUser().getId().equals(current.getId())) {
                notificationService.sendToUser(post.getUser().getId(), "POST_LIKE", "Lượt thích mới",
                        displayName(current) + " đã thích bài viết của bạn", current,
                        notificationService.data("postId", post.getId()));
            }
        }
        postRepository.save(post);
        return ResponseEntity.ok(ok("OK", toDto(post, current.getId())));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable Long postId) {
        User current = currentUser();
        List<PostComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<CommentDto> dtos = comments.stream().map(comment -> toCommentDto(comment, current.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(ok("OK", dtos));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable Long postId,
                                                           @RequestBody Map<String, String> body) {
        User current = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài viết không tồn tại"));
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bình luận là bắt buộc");
        }
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(current);
        comment.setContent(content.trim());
        PostComment saved = commentRepository.save(comment);

        // Notify post owner if commenter is not the post owner
        if (!post.getUser().getId().equals(current.getId())) {
            notificationService.sendToUser(post.getUser().getId(), "POST_COMMENT", "Bình luận mới",
                    displayName(current) + " đã bình luận về bài viết của bạn", current,
                    notificationService.data("postId", post.getId(), "commentId", saved.getId()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Đã bình luận", toCommentDto(saved, current.getId())));
    }

    @PostMapping("/{postId}/comments/{commentId}/reply")
    public ResponseEntity<Map<String, Object>> replyToComment(@PathVariable Long postId,
                                                              @PathVariable Long commentId,
                                                              @RequestBody Map<String, String> body) {
        User current = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài viết không tồn tại"));
        PostComment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bình luận không tồn tại"));
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bình luận không thuộc bài viết này");
        }
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung trả lời là bắt buộc");
        }
        PostComment reply = new PostComment();
        reply.setPost(post);
        reply.setUser(current);
        reply.setReplyToComment(parent);
        reply.setContent(content.trim());
        PostComment saved = commentRepository.save(reply);

        // Notify parent comment owner if replier is not parent comment owner
        if (!parent.getUser().getId().equals(current.getId())) {
            notificationService.sendToUser(parent.getUser().getId(), "COMMENT_REPLY", "Trả lời bình luận",
                    displayName(current) + " đã trả lời bình luận của bạn", current,
                    notificationService.data("postId", post.getId(), "commentId", saved.getId(), "parentId", parent.getId()));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Đã trả lời", toCommentDto(saved, current.getId())));
    }

    @PutMapping("/{postId}/comments/{commentId}/reaction")
    public ResponseEntity<Map<String, Object>> reactToComment(@PathVariable Long postId,
                                                              @PathVariable Long commentId,
                                                              @RequestBody Map<String, String> body) {
        User current = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BÃ i viáº¿t khÃ´ng tá»“n táº¡i"));
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BÃ¬nh luáº­n khÃ´ng tá»“n táº¡i"));
        if (!comment.getPost().getId().equals(post.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BÃ¬nh luáº­n khÃ´ng thuá»™c bÃ i viáº¿t nÃ y");
        }
        String reaction = body.get("reaction");
        if (reaction == null || reaction.trim().isEmpty()) {
            commentReactionRepository.deleteByCommentIdAndUserId(commentId, current.getId());
        } else {
            PostCommentReaction existing = commentReactionRepository
                    .findByCommentIdAndUserId(commentId, current.getId())
                    .orElseGet(PostCommentReaction::new);
            existing.setComment(comment);
            existing.setUser(current);
            existing.setReaction(reaction.trim());
            commentReactionRepository.save(existing);
        }
        return ResponseEntity.ok(ok("OK", toCommentDto(comment, current.getId())));
    }

    private PostDto toDto(Post p, Long currentUserId) {
        User author = p.getUser();
        int commentCount = (int) commentRepository.countByPostId(p.getId());
        return new PostDto(
                p.getId(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                p.getContent(),
                p.getMediaUrl(),
                p.getCreatedAt(),
                p.getLikedByUserIds().size(),
                p.getLikedByUserIds().contains(currentUserId),
                commentCount
        );
    }

    private CommentDto toCommentDto(PostComment c, Long currentUserId) {
        User author = c.getUser();
        Long replyToCommentId = c.getReplyToComment() != null ? c.getReplyToComment().getId() : null;
        long reactionCount = commentReactionRepository.countByCommentId(c.getId());
        String myReaction = null;
        if (currentUserId != null) {
            myReaction = commentReactionRepository.findByCommentIdAndUserId(c.getId(), currentUserId)
                    .map(PostCommentReaction::getReaction)
                    .orElse(null);
        }
        String replyToAuthorDisplayName = null;
        String replyToContent = null;
        if (c.getReplyToComment() != null) {
            User replyAuthor = c.getReplyToComment().getUser();
            replyToAuthorDisplayName = replyAuthor.getDisplayName() != null && !replyAuthor.getDisplayName().isBlank()
                    ? replyAuthor.getDisplayName()
                    : replyAuthor.getUsername();
            replyToContent = c.getReplyToComment().getContent();
        }
        return new CommentDto(
                c.getId(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                c.getContent(),
                c.getCreatedAt(),
                replyToCommentId,
                replyToAuthorDisplayName,
                replyToContent,
                commentRepository.countByReplyToCommentId(c.getId()),
                myReaction,
                reactionCount
        );
    }

    private String displayName(User user) {
        if (user == null) return "Ai do";
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return user.getUsername() != null ? user.getUsername() : "Ai do";
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Map<String, Object> ok(String msg, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        r.put("error", null);
        return r;
    }
}