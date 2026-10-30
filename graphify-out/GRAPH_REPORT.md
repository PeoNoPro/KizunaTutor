# Graph Report - src  (2026-10-30)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1994 nodes · 5322 edges · 131 communities (66 shown, 65 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 306 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ab6ddd3f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 31
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 38
- Community 39
- Community 40
- Community 41
- Community 42
- Community 43
- Community 44
- Community 45
- Community 46
- Community 47
- Community 48
- Community 49
- Community 50
- Community 51
- Community 52
- Community 53
- Community 54
- Community 55
- Community 56
- Community 57
- Community 58
- Community 59
- Community 60
- Community 61
- Community 62
- Community 63
- Community 64
- Community 65
- Community 66
- Community 67
- Community 68
- Community 69
- Community 70
- Community 71
- Community 72
- Community 73
- Community 74
- Community 75
- Community 76
- Community 77
- Community 78
- Community 79
- Community 80
- Community 81
- Community 82
- Community 83
- Community 84
- Community 85
- Community 86
- Community 87
- Community 88
- Community 89
- Community 90
- Community 91
- Community 92
- Community 93
- Community 94
- Community 95
- Community 96
- Community 97
- Community 98
- Community 99
- Community 100
- Community 101
- Community 102
- Community 103
- Community 104
- Community 105
- Community 106
- Community 107
- Community 108
- Community 109
- Community 110
- Community 111
- Community 112
- Community 113
- Community 114
- Community 115
- Community 116
- Community 117
- Community 118
- Community 119
- Community 120
- Community 121
- Community 122
- Community 123
- Community 124
- Community 125
- Community 127

## God Nodes (most connected - your core abstractions)
1. `User` - 53 edges
2. `KanjiDictionary` - 44 edges
3. `UserRepository` - 44 edges
4. `KanjiDictionaryRepository` - 42 edges
5. `SecurityContextUtil` - 32 edges
6. `TutorRepository` - 31 edges
7. `BadRequestException` - 31 edges
8. `HandwritingSearchService` - 31 edges
9. `BaseEntity` - 29 edges
10. `LearningSpaceRepository` - 26 edges

## Surprising Connections (you probably didn't know these)
- `AdminUserManagementTest` --references--> `User`  [EXTRACTED]
  test/java/com/kizuna/AdminUserManagementTest.java → main/java/com/kizuna/entity/User.java
- `AdminUserManagementTest` --references--> `UserRepository`  [EXTRACTED]
  test/java/com/kizuna/AdminUserManagementTest.java → main/java/com/kizuna/repository/UserRepository.java
- `EmailOtpRegistrationTest` --references--> `UserRepository`  [EXTRACTED]
  test/java/com/kizuna/EmailOtpRegistrationTest.java → main/java/com/kizuna/repository/UserRepository.java
- `LearningSpaceAccessControlTest` --references--> `LearningSpaceRepository`  [EXTRACTED]
  test/java/com/kizuna/LearningSpaceAccessControlTest.java → main/java/com/kizuna/repository/LearningSpaceRepository.java
- `OAuth2AndRegistrationTest` --references--> `UserRepository`  [EXTRACTED]
  test/java/com/kizuna/OAuth2AndRegistrationTest.java → main/java/com/kizuna/repository/UserRepository.java

## Import Cycles
- None detected.

## Communities (131 total, 65 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (48): AgentMemoryController, CreateUserRequest, UpdateStatusRequest, UpdateUserRoleRequest, UserPageResponse, ChatMemoryRequest, ChatMemoryResponse, FirstTimePasswordChangeRequest (+40 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (12): LoginRequest, LoginResponse, IOtpService, ActorSpecificViewsTest, AdminUserManagementTest, AuthAndDashboardRoutingTest, EmailOtpRegistrationTest, KizunaBackendApplicationTests (+4 more)

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (5): MinigameController, TutorProfileApiController, KanjiCardDto, IKanjiFolderService, IMinigameService

### Community 3 - "Community 3"
Cohesion: 0.09
Nodes (15): TutorRepository, TutorSecurityAspect, IAssignmentService, IKanjiPracticeService, IMembershipPaymentService, AssignmentServiceImpl, KanjiPracticeServiceImpl, MembershipPaymentServiceImpl (+7 more)

### Community 4 - "Community 4"
Cohesion: 0.10
Nodes (5): FallingKanji, KanjiRainGame, SoundEffects, SparkleParticle, SakuraSticker

### Community 5 - "Community 5"
Cohesion: 0.12
Nodes (4): AuthController, BadRequestException, IAuthService, AuthServiceImpl

### Community 6 - "Community 6"
Cohesion: 0.09
Nodes (5): OAuth2AuthenticationSuccessHandler, Learner, LearnerRepository, RoleRepository, AuthDataSeeder

### Community 7 - "Community 7"
Cohesion: 0.12
Nodes (30): closeQuickAddToFolderModal(), escapeHtml(), initKanjiFolderManager(), kfmAddKanjiToCurrentFolder(), kfmAddRandomKanjis(), kfmBuildKanjiCardHtml(), kfmCloseCreateModal(), kfmCloseEditModal() (+22 more)

### Community 8 - "Community 8"
Cohesion: 0.08
Nodes (8): MembershipPlan, TutorMembership, TutorMembershipState, ACTIVE, CANCELLED, EXPIRED, MembershipPlanRepository, TutorMembershipRepository

### Community 9 - "Community 9"
Cohesion: 0.15
Nodes (30): cleanKanjiVgSvg(), closeHwDetailModal(), closeHwSearchModal(), escapeHtml(), getHwCoords(), hwClearCanvas(), hwCurrentCandidates, hwCurrentStroke (+22 more)

### Community 10 - "Community 10"
Cohesion: 0.15
Nodes (30): cleanKanjiVgSvg(), closeHwDetailModal(), closeHwSearchModal(), escapeHtml(), getHwCoords(), hwClearCanvas(), hwCurrentCandidates, hwCurrentStroke (+22 more)

### Community 11 - "Community 11"
Cohesion: 0.15
Nodes (30): cleanKanjiVgSvg(), closeHwDetailModal(), closeHwSearchModal(), escapeHtml(), getHwCoords(), hwClearCanvas(), hwCurrentCandidates, hwCurrentStroke (+22 more)

### Community 12 - "Community 12"
Cohesion: 0.15
Nodes (8): KanjiPracticeSubmissionRepository, LearningMaterialRepository, LearningProgressRecordRepository, PaymentProcessingLogRepository, PaymentTransactionRepository, SupportTicketRepository, TutorCredentialRepository, UserNotificationRepository

### Community 13 - "Community 13"
Cohesion: 0.13
Nodes (11): AdminController, CsController, ManagerController, RoleEnum, ROLE_ACADEMIC_OFFICER, ROLE_ADMIN, ROLE_CS_STAFF, ROLE_LEARNER (+3 more)

### Community 16 - "Community 16"
Cohesion: 0.15
Nodes (3): AdminUserController, UserDTO, IAdminUserService

### Community 17 - "Community 17"
Cohesion: 0.17
Nodes (3): User, ResourceNotFoundException, AdminUserServiceImpl

### Community 18 - "Community 18"
Cohesion: 0.10
Nodes (4): MinigameMistakeLog, MinigameSession, MinigameMistakeLogRepository, MinigameSessionRepository

### Community 19 - "Community 19"
Cohesion: 0.13
Nodes (6): AiServiceWebClientConfig, AsyncConfig, OpenApiSwaggerConfig, PasswordEncoderConfig, VNPayConfig, WebMvcConfig

### Community 20 - "Community 20"
Cohesion: 0.17
Nodes (3): TutorController, TutorMembershipApiController, ITutorMembershipService

### Community 21 - "Community 21"
Cohesion: 0.19
Nodes (8): ActionableTip, ScoreDeduction, VisualHighlightZone, BoundingBox, GradingService, Point2D, StandardStroke, VectorTopologyResult

### Community 22 - "Community 22"
Cohesion: 0.17
Nodes (18): adminUserState, closeConfirmModal(), closeCreateUserModal(), closeEditUserModal(), confirmResendCredentials(), escapeHtml(), executeDeleteUser(), executeToggleStatus() (+10 more)

### Community 23 - "Community 23"
Cohesion: 0.17
Nodes (22): closeFolderManagerModal(), closeGameConfigModal(), closeGameOverModal(), closeGameWinModal(), currentLoadedFolders, deleteUserFolder(), exitToGameSetup(), fetchUserFoldersForSelect() (+14 more)

### Community 24 - "Community 24"
Cohesion: 0.13
Nodes (3): AdminDiagnosticEmailController, IEmailService, EmailServiceImpl

### Community 27 - "Community 27"
Cohesion: 0.13
Nodes (7): JoinSpaceRequest, LearningSpaceController, LearnerAccessState, ACTIVE_OFFICIAL_LEARNER, UNASSIGNED_LEARNER, UNAUTHENTICATED, ILearningSpaceService

### Community 28 - "Community 28"
Cohesion: 0.15
Nodes (13): animateSingleStrokeToCanvas(), clearCanvas(), completeNextStroke(), draw(), ensureCanvasReady(), getPos(), redrawAllStrokes(), replayCanvasGuideAnimation() (+5 more)

### Community 30 - "Community 30"
Cohesion: 0.11
Nodes (3): BaseEntity, LearningProgressRecord, TutorCredential

### Community 31 - "Community 31"
Cohesion: 0.22
Nodes (8): AdminSystemController, AssignmentController, MembershipPaymentController, SupportTicketController, TestController, TrialBookingController, TutorSearchController, UserProfileController

### Community 32 - "Community 32"
Cohesion: 0.16
Nodes (3): KanaProgressRequest, LearnerKanjiApiController, SaveToDeckRequest

### Community 34 - "Community 34"
Cohesion: 0.12
Nodes (7): LearningSpace, LearningSpaceStatus, ACTIVE, CLOSED, COMPLETED, PAUSED, LearningSpaceRepository

### Community 35 - "Community 35"
Cohesion: 0.20
Nodes (16): checkIfOtpFull(), closeOtpModal(), DASHBOARD_ROUTES, handleResendOtp(), hideOtpAlert(), initOtpInputs(), initRegistrationForm(), openOtpModal() (+8 more)

### Community 36 - "Community 36"
Cohesion: 0.12
Nodes (6): TutorState, ACTIVE_PAID, APPROVED_UNPAID, DRAFT_PROFILE, PENDING_VERIFICATION, REJECTED

### Community 37 - "Community 37"
Cohesion: 0.21
Nodes (3): ScoreCriterion, ScoreResult, AiStrokeAnalysisService

### Community 38 - "Community 38"
Cohesion: 0.18
Nodes (4): KanjiDataSeeder, BatchPreparedStatementSetter, TypeReference<>, OfflineKanjiData

### Community 40 - "Community 40"
Cohesion: 0.20
Nodes (16): clearAuthSession(), DASHBOARD_ROUTES, firstTimeAuthData, getAuthToken(), getAuthUser(), handleFirstTimeChangePassword(), handleLoginSubmit(), handleLogout() (+8 more)

### Community 42 - "Community 42"
Cohesion: 0.21
Nodes (3): VnPayServiceImpl, IVnPayService, TutorMembershipServiceImpl

### Community 47 - "Community 47"
Cohesion: 0.21
Nodes (6): UserAccountStatus, ACTIVE, BANNED, PENDING, SUSPENDED, UserRepository

### Community 48 - "Community 48"
Cohesion: 0.27
Nodes (9): findNextUnsolvedCardIndex(), getKanaItems(), recalculateSelectedKana(), renderButtonStates(), renderSelectionMatrix(), syncSelectedGroupsWithChars(), updateArenaProgressBar(), updateSelectionCounter() (+1 more)

### Community 50 - "Community 50"
Cohesion: 0.30
Nodes (10): escapeHtml(), loadKanjiCatalog(), onLibKeywordInput(), onLibStrokeCountInput(), onResetLibFilters(), onSelectJlptLevel(), renderKanjiCatalogGrid(), renderPagination() (+2 more)

### Community 70 - "Community 70"
Cohesion: 0.20
Nodes (10): base-url, app, agent-memory, jwt, seeder, expiration-ms, secret, batch-size (+2 more)

### Community 71 - "Community 71"
Cohesion: 0.20
Nodes (10): smtp, auth, connectiontimeout, ssl, starttls, timeout, writetimeout, trust (+2 more)

### Community 73 - "Community 73"
Cohesion: 0.22
Nodes (8): SupportTicketCategory, ACCOUNT, BILLING, BOOKING, MEMBERSHIP, OTHER, TECHNICAL, TUTOR

### Community 75 - "Community 75"
Cohesion: 0.39
Nodes (3): HandwritingSearchService, OfflineKanji, ScoredKanji

### Community 84 - "Community 84"
Cohesion: 0.25
Nodes (6): TutorVerificationStatus, ADDITIONAL_INFO_REQUIRED, APPROVED, PENDING, REJECTED, UNVERIFIED

### Community 86 - "Community 86"
Cohesion: 0.25
Nodes (8): registration, client-id, client-secret, redirect-uri, scope, client, google, oauth2

### Community 87 - "Community 87"
Cohesion: 0.25
Nodes (8): default-encoding, host, password, port, properties, username, mail, mail

### Community 88 - "Community 88"
Cohesion: 0.29
Nodes (6): AssignmentSubmissionStatus, AI_EVALUATED, FINALIZED, GRADED, GRADING, SUBMITTED

### Community 89 - "Community 89"
Cohesion: 0.29
Nodes (6): ConnectionRequestStatus, ACCEPTED, CANCELLED, DECLINED, EXPIRED, PENDING

### Community 90 - "Community 90"
Cohesion: 0.29
Nodes (6): JlptLevel, N1, N2, N3, N4, N5

### Community 91 - "Community 91"
Cohesion: 0.29
Nodes (6): NotificationType, ASSIGNMENT, PAYMENT, SYSTEM, TICKET, TRIAL

### Community 92 - "Community 92"
Cohesion: 0.29
Nodes (6): SupportTicketStatus, CLOSED, ESCALATED, IN_PROGRESS, OPEN, RESOLVED

### Community 93 - "Community 93"
Cohesion: 0.29
Nodes (6): TrialBookingStatus, CANCELLED, COMPLETED, CONFIRMED, PENDING, RESCHEDULED

### Community 95 - "Community 95"
Cohesion: 0.62
Nodes (4): CharacterSensoryReport, StrokePoint, StrokeSensoryAnalyzer, StrokeSensoryResult

### Community 96 - "Community 96"
Cohesion: 0.29
Nodes (6): charset, force, server, port, servlet, encoding

### Community 97 - "Community 97"
Cohesion: 0.29
Nodes (7): driver-class-name, password, url, username, spring, datasource, security

### Community 98 - "Community 98"
Cohesion: 0.29
Nodes (7): ddl-auto, dialect, hibernate, properties, show-sql, hibernate, jpa

### Community 100 - "Community 100"
Cohesion: 0.33
Nodes (5): PaymentTransactionStatus, FAILED, PENDING, REFUNDED, SUCCESS

### Community 101 - "Community 101"
Cohesion: 0.33
Nodes (5): PracticeSubmissionStatus, COMPLETED, FAILED, PENDING, PROCESSING

### Community 102 - "Community 102"
Cohesion: 0.33
Nodes (5): SupportTicketPriority, HIGH, LOW, MEDIUM, URGENT

### Community 103 - "Community 103"
Cohesion: 0.33
Nodes (5): TutorMembershipStatus, EXPIRED, FREE, PREMIUM, STANDARD

### Community 104 - "Community 104"
Cohesion: 0.33
Nodes (5): TutorVerifAction, APPROVE, REJECT, REQUEST_INFO, SUBMIT

### Community 106 - "Community 106"
Cohesion: 0.33
Nodes (6): thymeleaf, cache, encoding, mode, prefix, suffix

### Community 107 - "Community 107"
Cohesion: 0.67
Nodes (5): alert(), close(), confirm(), ensureDialogDOM(), show()

### Community 110 - "Community 110"
Cohesion: 0.40
Nodes (4): TutorCredentialStatus, PENDING, REJECTED, VERIFIED

### Community 112 - "Community 112"
Cohesion: 0.40
Nodes (5): gemini-api-key, gemini-model, openai-api-key, provider, ai

### Community 113 - "Community 113"
Cohesion: 0.60
Nodes (3): ensureContainer(), escapeHtml(), show()

### Community 114 - "Community 114"
Cohesion: 0.60
Nodes (3): ensureContainer(), escapeHtml(), show()

## Knowledge Gaps
- **90 isolated node(s):** `FAILED`, `PENDING`, `REFUNDED`, `SUCCESS`, `COMPLETED` (+85 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 617 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **65 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `User` connect `Community 17` to `Community 1`, `Community 2`, `Community 3`, `Community 5`, `Community 6`, `Community 39`, `Community 74`, `Community 43`, `Community 12`, `Community 15`, `Community 47`, `Community 16`, `Community 18`, `Community 82`, `Community 53`, `Community 94`, `Community 27`, `Community 30`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **What connects `FAILED`, `PENDING`, `REFUNDED` to the rest of the system?**
  _90 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.06346708628376606 - nodes in this community are weakly interconnected._
- **Why does `BaseEntity` connect `Community 30` to `Community 17`, `Community 34`, `Community 53`, `Community 55`, `Community 56`, `Community 57`, `Community 58`, `Community 59`, `Community 60`, `Community 61`, `Community 62`, `Community 63`, `Community 64`, `Community 65`, `Community 66`, `Community 67`, `Community 68`, `Community 78`, `Community 79`, `Community 80`, `Community 81`, `Community 83`, `Community 85`, `Community 108`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.05388961321164711 - nodes in this community are weakly interconnected._
- **Why does `UserRepository` connect `Community 47` to `Community 1`, `Community 2`, `Community 3`, `Community 5`, `Community 6`, `Community 39`, `Community 43`, `Community 12`, `Community 13`, `Community 15`, `Community 17`, `Community 52`, `Community 94`?**
  _High betweenness centrality (0.034) - this node is a cross-community bridge._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.05933094887439512 - nodes in this community are weakly interconnected._