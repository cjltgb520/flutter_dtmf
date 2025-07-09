package com.dormmom.flutter_dtmf;

import android.media.AudioManager;
import android.media.ToneGenerator;
import androidx.annotation.NonNull; // 保留此导入，用于 @NonNull 注解

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * FlutterDtmfPlugin
 */
// 类已经正确实现了 FlutterPlugin 和 MethodCallHandler
public class FlutterDtmfPlugin implements FlutterPlugin, MethodCallHandler {
    private MethodChannel channel; // 在类级别声明 channel，以便在 onDetachedFromEngine 中访问
    private ToneGenerator generator;
    private ToneGenerator voiceGenerator;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        // 获取 BinaryMessenger 的正确方式
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_dtmf");
        // 将当前插件实例设置为 MethodCallHandler
        channel.setMethodCallHandler(this);

        // 如果生成器需要与插件的生命周期绑定，则在此处初始化它们
        // 注意：原始代码是在 playVoiceTone/playTone 内部惰性初始化的。
        // 如果它们需要在插件连接时始终可用，则在此处初始化。
        // 否则，保持惰性初始化，但要确保正确释放。
    }

    // 移除整个静态块：这是旧的注册方法。
    /*
    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_dtmf");
        channel.setMethodCallHandler(new FlutterDtmfPlugin());
    }
    */

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "playTone":
                String digits = call.argument("digits");
                if (digits != null) {
                    playTone(digits);
                }
                result.success(null);
                break;
            case "playCallWaiting":
                playVoiceTone(ToneGenerator.TONE_SUP_RINGTONE, 1000);
                result.success(null);
                break;
            case "playCallAlert":
                playVoiceTone(ToneGenerator.TONE_SUP_CONFIRM, 100);
                result.success(null);
                break;
            case "playCallTerm":
                playVoiceTone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, 200);
                result.success(null);
                break;
            case "releaseGenerator":
                if (generator != null) {
                    generator.stopTone();
                    generator.release();
                    generator = null;
                }
                if (voiceGenerator != null) {
                    voiceGenerator.stopTone();
                    voiceGenerator.release();
                    voiceGenerator = null;
                }
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void playVoiceTone(int toneType, int durationMs) {
        try {
            if (generator == null) {
                generator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            }
            generator.startTone(toneType, durationMs);
        } catch (Exception ignore) {
        }
    }

    private void playTone(String digits) {
        try {
            if (voiceGenerator == null) {
                voiceGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            }
            for (int i = 0; i < digits.length(); i++) {
                int toneType = getToneType(digits.charAt(i));
                // 检查 toneType 是否有效（-1 表示无效数字）
                if (toneType != -1) {
                    voiceGenerator.startTone(toneType, 200);
                }
            }
        } catch (Exception ignore) {
            // 考虑记录异常以便调试
        }
    }

    private int getToneType(char digit) {
        switch (digit) {
            case '0':
                return ToneGenerator.TONE_DTMF_0;
            case '1':
                return ToneGenerator.TONE_DTMF_1;
            case '2':
                return ToneGenerator.TONE_DTMF_2;
            case '3':
                return ToneGenerator.TONE_DTMF_3;
            case '4':
                return ToneGenerator.TONE_DTMF_4;
            case '5':
                return ToneGenerator.TONE_DTMF_5;
            case '6':
                return ToneGenerator.TONE_DTMF_6;
            case '7':
                return ToneGenerator.TONE_DTMF_7;
            case '8':
                return ToneGenerator.TONE_DTMF_8;
            case '9':
                return ToneGenerator.TONE_DTMF_9;
            case '*':
                return ToneGenerator.TONE_DTMF_S;
            case '#':
                return ToneGenerator.TONE_DTMF_P;
            case 'A':
                return ToneGenerator.TONE_DTMF_A;
            case 'B':
                return ToneGenerator.TONE_DTMF_B;
            case 'C':
                return ToneGenerator.TONE_DTMF_C;
            case 'D':
                return ToneGenerator.TONE_DTMF_D;
            default:
                return -1;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        // 至关重要：当引擎分离时清理资源
        channel.setMethodCallHandler(null); // 取消设置处理器以避免内存泄漏
        if (generator != null) {
            generator.release();
            generator = null;
        }
        if (voiceGenerator != null) {
            voiceGenerator.release();
            voiceGenerator = null;
        }
        // 如果 'binding' 未存储为类成员，则无需清除它
    }
}