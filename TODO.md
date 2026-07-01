# TODO - Fix call mic permission stability (Xiaomi/Huawei)

- [x] Add mic permission pre-check in `OutgoingCallActivity` before starting call flow
- [x] Add mic permission pre-check in `IncomingCallActivity` before answering call
- [x] Add helper/deep-link to App Settings when mic permission is denied
- [x] Add local audio readiness check in `WebRtcManager`
- [x] Wire fail-fast behavior when local audio is not ready
- [ ] Build/check compile for Android app
- [ ] Run critical-path verification summary
