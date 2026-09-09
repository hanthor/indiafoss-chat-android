# Fork snapshot baseline review — 8 September 2026

Source: [CI run 34243134153](https://github.com/hanthor/indiafoss-chat-android/actions/runs/34243134153),
head `d07f26fa5fdb0adb406806db195b71a8fb772cdf`, artifact
`tests-and-screenshot-tests-results` (10063760304). The UI suite executed
3,017 cases, with 66 failures; enterprise tests independently exposed a stale
empty-homeserver expectation. These are execution failures after LFS recovery,
not missing LFS objects.

Reviewed all 66 actual images in six comparison sheets, plus representative
expected/delta/actual images. The changes match the fork's existing display-name
and discovery onboarding, nearby-discovery settings, developer peer list,
contact-code scanner, contact code and mesh profile previews. Loading/error,
empty/populated, enabled/disabled and light/dark/black variants were inspected.
The advanced settings delta is the added nearby-discovery section; newly added
previews had no expected image. Camera placeholders are preview fixtures.

Copied only the reviewed actual PNGs from that exact CI artifact, preserving
bytes and Git LFS tracking. No render thresholds, test exclusions, application
code or coverage requirements changed. The table records source hashes so the
accepted images can be audited against the artifact. CI must rerun successfully
before this repair is merged. This review does not verify camera hardware, mesh
connectivity or account identity binding.

35 baselines were absent; 31 existing baselines were updated.

| Image | SHA-256 |
| --- | --- |
| `features.ftue.impl.discovery_DiscoveryOptInView_Day_0_en.png` | `b67b371f9e1e5de8a7a21bc44645b829d9d0ffc34b2fca2fe3a64b1f9aae3053` |
| `features.ftue.impl.discovery_DiscoveryOptInView_Day_1_en.png` | `9e3bddca31fa96f7ab93630f3bd44b90ef7565416ed4324f97c0e614f2206416` |
| `features.ftue.impl.discovery_DiscoveryOptInView_Night_0_en.png` | `6468022d1956ef425204558eed6a88df107cfa6bdc96457d59f4b913dc0a302e` |
| `features.ftue.impl.discovery_DiscoveryOptInView_Night_1_en.png` | `6cc96f555567210ca7230ca8630e8fcd66571ecfe2251d4132b3c54be2ee6752` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Day_0_en.png` | `e73b22508139ae6fbc93637000f4a39d18180fd813e4ad5d5b44db97005bd0d9` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Day_1_en.png` | `94f2aaaf7162b9658377cbed0ca6e6d569d033defcee45169a2ccc4f9f40bcbc` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Day_2_en.png` | `c9b3918a1a2ad717c6cc050f0a994a0efb27ad019e4bf16bf4d324551b00c4ce` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Day_3_en.png` | `9fa99c98e68544a13bd0ffd84ece9e6f1191bc8e869f46b676f8b9ddb31e29a5` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Night_0_en.png` | `6649e1d769a57ce9acd5440b7b14b1aead202456a328254cf9ca599efa569387` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Night_1_en.png` | `fe5bab3e83110a6f3a33a25302b20d5cab52048c9b93c51c8d81c04d7bb2e468` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Night_2_en.png` | `5acf0b35b488fca7c723f99743586cc431de615cd350eed7155410ab2b389e15` |
| `features.ftue.impl.setdisplayname_SetDisplayNameView_Night_3_en.png` | `7798bd81da8670873c8c410a13912316dd9d9218981fcc4c27d4d6fc81c68dbf` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_0_en.png` | `93b6546450ef77b50fef130a46da07bb25849433aac8b21d1f2d33245619a7b6` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_1_en.png` | `09cbcae16df1a5ebcbe1966df0b283c12fe2f2ed2628bcfd391347a8bbfbd22e` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_2_en.png` | `d897ac31e5f8bf57341f5e90fa7ee3a8ce3704ba2107f637aefe345380d72e1b` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_3_en.png` | `b425392713147e7c6c61f4d49731f207f983c519dfba55aca41401aeaa0e5fcd` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_4_en.png` | `d904c499a36a9fda8b2923c4d5b89c995f047ff413461aa78bf9c607913fd247` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_5_en.png` | `bc16ddbc7bbccd81ee8f5c9e34485b2f39038d2f0852bc65659542b4408f2081` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_6_en.png` | `f855c4c69f0ad3813a07fda2215fa6543d73403cb6d748d2a8899ac6e0854324` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_7_en.png` | `5118686de4101396fad5b79ac2d169152b526dbf0679822d0f07b3bdb76371c2` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_8_en.png` | `f0a8320f5ed9c8652b92205caff20d575f8e2a6cd7ee9f50980c02ce1cf82bd1` |
| `features.preferences.impl.advanced_AdvancedSettingsViewBlack_9_en.png` | `30e0b8e9328645bf0fc65224458a3ec4a6356e15bb6b02abd1f56344ec38ce2f` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_0_en.png` | `5266e54e23e011b2e1644d6240bfbe239e62078187f7a6c55481d3d7edd8d892` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_1_en.png` | `81499a2394657033acad66bfaab5499eb4a14479be8b3a063782f1f36911ac27` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_2_en.png` | `683aae4d76047f80ead005895bf8dc927db17e57742a26bcb1c7ee5d9d147ebc` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_3_en.png` | `ef395572939f4547f046bb14f5a7b9207ffaee912d93d8bfe9e94f05e7424ef8` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_4_en.png` | `df175fcb5a8bdca71d6d0434ef2e649edda6cbfc72a12c0acdce0d4d6bca8b24` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_5_en.png` | `b5ac6515bd1ed649b38fb091c8bf12817235f65c7b3b443f20032100f39cccee` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_6_en.png` | `1aa9e44fc193aaa0a50bbab12ff95e1f7a357e7c8c8071f473a9fde2aa6958fc` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_7_en.png` | `0526a5575160c8ce74042421eb5464c180470a17b90c30297183e1d86a00ce75` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_8_en.png` | `144dd57bd3b6804b060114bb93b71a91bb82885e97e570c61f2d1c965db547f5` |
| `features.preferences.impl.advanced_AdvancedSettingsViewDark_9_en.png` | `5621615ab5c950d048fac1423bab5f9d0c6715c26b6ff1cebcff3a0971469628` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_0_en.png` | `9868dd5880296ad39e2a0ac851fb24186f2e4987a7699e94feff0c888bc3bd91` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_1_en.png` | `f6d0a08239ba48d5cba5d3aa18cf422e2823aa6ea3d22f6e50ff84cfcb193f7a` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_2_en.png` | `0ae74284ffad52bbd243964b9faf700c9432f2e2e113818440e46568ad54c2d5` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_3_en.png` | `eb7e443cac82eed9c9ad1fbc8ec6b50435e8fa7415f5fe2aafaec1872c16c10e` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_4_en.png` | `1eae8dfd727fa7de6a48d267da0436c9c242a8eb6614dc91b193d74d4ad81817` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_5_en.png` | `404380a714a6c7cf768636a32f05d4c92f6c7da2a1695d3aa167e7bd23f5eff8` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_6_en.png` | `b3efe6574f0684ee1bb0af038f5a9ba1aee92bd05724076cd3a46a1961cb4fb6` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_7_en.png` | `948a0c94653c2b39d16475ef9558d69441982d46fafef61b53c3bd259bdc54e6` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_8_en.png` | `9fd851b0bdbb7e6ece59f4519b6059b1ad3dfc3db5fc47241dc658e5b0679f99` |
| `features.preferences.impl.advanced_AdvancedSettingsViewLight_9_en.png` | `f2545f717c4bc41c362d3c24a19fe672f46c3fd76ced0c2c44e7ee968bff96ff` |
| `features.preferences.impl.developer.neutrino_NeutrinoPeersView_Day_0_en.png` | `3378b5d0f7f2234a4e44e4139c5dc480969d4e008d7cd3ca2610e32b5a8126a1` |
| `features.preferences.impl.developer.neutrino_NeutrinoPeersView_Day_1_en.png` | `6ff4ce2966f49a21d5972efa3cd526ce97fa1dfb3d9001d3aa3a7bb86345aea3` |
| `features.preferences.impl.developer.neutrino_NeutrinoPeersView_Night_0_en.png` | `d6c2421bc1c4e5d7268895032dedf1e7d1ab0a7e2d7f791f68a35640960f69ff` |
| `features.preferences.impl.developer.neutrino_NeutrinoPeersView_Night_1_en.png` | `97da149190f116a97a3286da02754e6d68297af51b867a2478794ea543d5e6d7` |
| `features.preferences.impl.developer_DeveloperSettingsView_Day_3_en.png` | `87dbf550bb99c79e75d768858b20eef1b948e9053c508d31947fe34cf2caf8a0` |
| `features.preferences.impl.developer_DeveloperSettingsView_Night_3_en.png` | `17b52196e5c81b670514e47a9101823b0774caa5d27d00a3fb78e599551476c4` |
| `features.startchat.impl.root_StartChatView_Day_0_en.png` | `a6f59e4b5109ccf672153c883008fee3bd47d8ff6e4241021cb82c69d1dc3488` |
| `features.startchat.impl.root_StartChatView_Day_3_en.png` | `62d590bd5814af882dd38d08df13e06f5358ad198ff2f97ba36f1dd6dda24f50` |
| `features.startchat.impl.root_StartChatView_Night_0_en.png` | `fa9084228153aa9357b65e9e61b90e7b564d2b0773d6e99aaae7d0888729a2e8` |
| `features.startchat.impl.root_StartChatView_Night_3_en.png` | `18b9ed1ac0c7a2a07a01e293359ede8b883f37a5626eace9ccce6d09d3bd2d00` |
| `features.startchat.impl.scanqr_ScanQrView_Day_0_en.png` | `0f739744e0238da3a8cf8b1682f0199c0c8e04bd46b9f428acef760f11e0c885` |
| `features.startchat.impl.scanqr_ScanQrView_Day_1_en.png` | `edc6b6cac4d1ed002dd2684cdf61caa9d0817163943940aa37a5f1c461370792` |
| `features.startchat.impl.scanqr_ScanQrView_Day_2_en.png` | `2d731b8cbee2743b0dc0670a8781b0301094acf51c954837044f0977da7316f0` |
| `features.startchat.impl.scanqr_ScanQrView_Day_3_en.png` | `e98a7688476724f0c4f377accc907878158af2abfae3bd4c49b7e584a6a2ac2b` |
| `features.startchat.impl.scanqr_ScanQrView_Day_4_en.png` | `2a7f4f316eb7ca1f84270804a325ca3d2646053977b81b5763af11c6114f5784` |
| `features.startchat.impl.scanqr_ScanQrView_Night_0_en.png` | `abdbb822d6d148642fc63f59f5149a043036174f024b8f858814439ee43509a1` |
| `features.startchat.impl.scanqr_ScanQrView_Night_1_en.png` | `f25d7b4da65fdd85ead9c8e0f8fcd4131fa04946deed71aac03e471c9afcdfd4` |
| `features.startchat.impl.scanqr_ScanQrView_Night_2_en.png` | `63cb29955f4add81ef6819b91918fdb446de39ef1363173b2fd18cae3183eceb` |
| `features.startchat.impl.scanqr_ScanQrView_Night_3_en.png` | `fc2741310c331dd9e2f52a58beca121847024aa98c65842a0cf148a57b92e646` |
| `features.startchat.impl.scanqr_ScanQrView_Night_4_en.png` | `46d67ec8fa23cfb4b38bb65d736fbf7af475c7b2c96dcb7176796e00e61053f1` |
| `features.userprofile.shared_MyMeshCodeView_Day_0_en.png` | `1b0b1494e6287ac1525da252f90f7c0f4d8b23a5c0b04b89d86a473140d9ce5b` |
| `features.userprofile.shared_MyMeshCodeView_Night_0_en.png` | `8174134c251024fc18381846b04f80371576e6cf0d0c8346f789bd9104df6032` |
| `features.userprofile.shared_UserProfileHeaderSectionMesh_Day_0_en.png` | `27cad40936b236fab4e97d61254d182e7c9ff26f67079c12d4512a275855e35f` |
| `features.userprofile.shared_UserProfileHeaderSectionMesh_Night_0_en.png` | `81aa7c22667fc2b495ad2f5597a32a916c006d43630e3ab520ba6e5366e056e8` |
