# Feature Expansion Plan

Mục tiêu: mở rộng phần Bảng tin, Comments, Stories và danh sách chat theo hướng giống Messenger hơn, nhưng làm theo từng phase để dễ kiểm soát.

## Hiện trạng nhanh

- Feed đã có `posts`, `comments`, `likes`, `stories`
- Chat đã có `reply`, `reaction`, WebSocket backend, polling fallback ở Android
- Danh sách conversation đã có trạng thái online/last seen
- Chưa có:
  - reply cho comment
  - reaction cho comment
  - reaction/reply cho story
  - đẩy story reply vào chat
  - badge số tin nhắn chưa đọc trên conversation list

## Phase 1 - Comment System

Mục tiêu:
- cho phép reply một comment
- cho phép thả icon/reaction vào comment

Cần làm:
- Thêm schema cho comment reply và comment reaction
- Thêm API backend:
  - `GET /api/posts/{postId}/comments`
  - `POST /api/posts/{postId}/comments`
  - `POST /api/comments/{commentId}/reply`
  - `PUT /api/comments/{commentId}/reaction`
- Cập nhật Android:
  - UI hiển thị reply thread của comment
  - UI nút reaction cho comment
  - composer reply trong dialog comments

## Phase 2 - Story Interaction

Mục tiêu:
- xem story của người khác
- thả icon cho story
- comment/reply story
- reply story sẽ chuyển thành chat/message gửi tới chủ story

Cần làm:
- Thêm schema cho story reactions và story replies
- Thêm API backend cho story reply/reaction
- Cập nhật `StoryViewerActivity`
- Khi reply story, mở/đi tới conversation chat với chủ story

## Phase 3 - Unread Count For Conversations

Mục tiêu:
- danh sách chat hiển thị số tin nhắn chưa đọc
- trạng thái online vẫn giữ nguyên

Cần làm:
- Thêm đếm unread ở backend
- Lưu last read per user/conversation
- Trả về unread count trong `GET /api/conversations`
- Cập nhật `ConversationAdapter` hiển thị badge unread

## Phase 4 - UI Polish

Cần làm:
- đồng bộ màu nút, card, bubble, popup
- làm rõ trạng thái action/reaction
- tinh chỉnh layout trên điện thoại thật

## Thứ tự triển khai đề xuất

1. Comment reply + reaction
2. Story reply + reaction
3. Story reply sang chat
4. Unread badge trong danh sách chat
5. UI polish


## Tiến độ hiện tại

- Phase 1: backend comment reply/reaction đã được nối xong, Android cũng đã có composer reply và khay chọn reaction cơ bản.
- Phase 2, 3, 4: chưa làm, sẽ đi tiếp theo thứ tự trong plan.

## Tiến độ hiện tại - 2026-06-28

- Phase 1: comment reply/reaction đã xong.
- Phase 2: story viewer reply/react đã được nối sang chat riêng của chủ story.
- Phase 3, 4: còn lại.

## Tiến độ hiện tại - 2026-06-28 2

- Phase 1: comment reply/reaction đã xong.
- Phase 2: story reply/react đã xong.
- Phase 3: unread badge cho conversation list đã xong ở backend + Android.
- Phase 4: UI polish còn lại.

## Tiến độ hiện tại - 2026-06-28 3

- Phase 4: UI polish đã được cập nhật ở conversation list, comment card, story viewer và dialog tạo bài viết.

## Tiến độ hiện tại - 2026-06-28 4

- Conversation list có thêm preview tin nhắn mới nhất.
- Unread badge vẫn giữ nguyên.
