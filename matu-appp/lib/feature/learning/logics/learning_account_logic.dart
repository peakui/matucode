import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:image_picker/image_picker.dart';
import 'package:package_info_plus/package_info_plus.dart';
import '../../../core/base/base/base_logic.dart';
import '../../../core/data/repository/learning_repository.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../states/learning_states.dart';
import '../localization/learning_strings.dart';

/// 登录、个人资料与学习统计操作。
class LearningAccountLogic extends BaseLogic {
  LearningAccountLogic({
    this.mode = 'me',
    LearningRepository? repository,
    MatuSessionService? session,
  }) : _learningRepository = repository ?? LearningRepository(session: session),
       session = session ?? Get.find<MatuSessionService>();
  final String mode;
  final LearningRepository _learningRepository;
  final MatuSessionService session;
  final LearningAccountState state = LearningAccountState();
  final accountController = TextEditingController();
  final passwordController = TextEditingController();
  final nicknameController = TextEditingController();
  final signatureController = TextEditingController();
  Worker? _worker;
  @override
  void onInit() {
    super.onInit();
    if (mode != 'login') {
      _worker = ever(session.revision, (_) {
        load();
      });
    }
  }

  @override
  void initData() {
    if (mode == 'settings') {
      loadVersion();
    }
    if (mode != 'login') load();
  }

  /// 从安装包元数据读取版本。
  Future<void> loadVersion() async {
    try {
      final info = await PackageInfo.fromPlatform();
      if (!isClosed) {
        state.version.value = '${info.version}+${info.buildNumber}';
      }
    } on Object catch (e) {
      if (!isClosed) state.error.value = e.toString();
    }
  }

  Future<void> load() async {
    state.stats.clear();
    state.profile.value = null;
    state.error.value = '';
    if (!session.signedIn) return;
    final revision = session.revision.value;
    state.busy.value = true;
    try {
      final profile = await _learningRepository.profile();
      if (revision != session.revision.value || isClosed) return;
      state.profile.value = profile;
      nicknameController.text = profile.nickname;
      signatureController.text = profile.signature;
      await session.updateProfile(profile);
      if (mode == 'me') {
        final stats = await _learningRepository.statistics(
          session.user.value!.id,
        );
        if (revision == session.revision.value && !isClosed) {
          state.stats.assignAll(stats);
        }
      }
    } on Object catch (e) {
      if (revision == session.revision.value && !isClosed) {
        state.error.value = e.toString();
      }
    } finally {
      if (!isClosed) state.busy.value = false;
    }
  }

  Future<void> login() async {
    if (state.busy.value) return;
    if (accountController.text.trim().isEmpty ||
        passwordController.text.isEmpty) {
      state.error.value = LearningStrings.loginValidation;
      return;
    }
    state.busy.value = true;
    state.error.value = '';
    try {
      final result = await _learningRepository.login(
        accountController.text.trim(),
        passwordController.text,
      );
      if (isClosed) return;
      await session.accept(result);
      passwordController.clear();
      Get.back(result: true);
    } on Object catch (e) {
      if (!isClosed) state.error.value = e.toString();
    } finally {
      if (!isClosed) state.busy.value = false;
    }
  }

  Future<void> save() async {
    if (!await LearningNavigator.requireLogin() || state.busy.value) return;
    if (nicknameController.text.trim().isEmpty) {
      state.error.value = LearningStrings.nickname;
      return;
    }
    await _mutate(
      () => _learningRepository.saveProfile(
        nicknameController.text.trim(),
        signatureController.text.trim(),
      ),
    );
  }

  Future<void> avatar() async {
    if (!await LearningNavigator.requireLogin() || state.busy.value) return;
    try {
      final revision = session.revision.value;
      final image = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 1024,
        maxHeight: 1024,
        imageQuality: 85,
      );
      if (image == null || revision != session.revision.value || isClosed) {
        return;
      }
      final bytes = await image.readAsBytes();
      if (revision != session.revision.value || isClosed) return;
      await _mutate(() => _learningRepository.uploadAvatar(bytes, image.name));
    } on Object catch (e) {
      state.error.value = e.toString();
    }
  }

  Future<void> _mutate(Future<MatuProfile> Function() operation) async {
    state.busy.value = true;
    state.error.value = '';
    state.message.value = '';
    final revision = session.revision.value;
    try {
      final profile = await operation();
      if (revision != session.revision.value || isClosed) return;
      await session.updateProfile(profile);
      state.profile.value = profile;
      state.message.value = LearningStrings.saved;
    } on Object catch (e) {
      if (!isClosed) state.error.value = e.toString();
    } finally {
      if (!isClosed) state.busy.value = false;
    }
  }

  Future<void> logout() async {
    if (state.busy.value) return;
    final confirmed = await Get.dialog<bool>(
      AlertDialog(
        title: const Text(LearningStrings.logout),
        content: const Text(LearningStrings.logoutConfirm),
        actions: [
          TextButton(
            onPressed: () => Get.back(result: false),
            child: const Text(LearningStrings.cancel),
          ),
          TextButton(
            onPressed: () => Get.back(result: true),
            child: const Text(LearningStrings.confirm),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    state.busy.value = true;
    try {
      await _learningRepository.logout();
    } on Object {
      /* 本机退出不依赖网络可用性。 */
    } finally {
      await session.clear();
      state.stats.clear();
      state.profile.value = null;
      state.busy.value = false;
    }
  }

  @override
  void onClose() {
    _worker?.dispose();
    accountController.dispose();
    passwordController.dispose();
    nicknameController.dispose();
    signatureController.dispose();
    super.onClose();
  }
}
