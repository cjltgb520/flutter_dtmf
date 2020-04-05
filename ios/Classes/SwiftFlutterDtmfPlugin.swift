import Flutter
import UIKit
import AVFoundation
import CallKit

public class SwiftFlutterDtmfPlugin: NSObject, FlutterPlugin {
    var _engine: AVAudioEngine
    var _player:AVAudioPlayerNode
    var _mixer: AVAudioMixerNode
    
    var isSwitchSpeaker:Bool

    public override init() {
        _engine = AVAudioEngine();
        _player = AVAudioPlayerNode()
        _mixer = _engine.mainMixerNode;
        isSwitchSpeaker=false;
        print("SwiftFlutterDtmfPlugin init")
        super.init()
    }

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_dtmf", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterDtmfPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let arguments = call.arguments as? NSDictionary
        if call.method == "playTone"
        {
            guard let digits = arguments?["digits"] as? String else {return}
            AudioServicesPlaySystemSound(DTMF.toneSoundForString(stringForTone: digits) ?? DTMF.toneSound0)
            //            let samplingRate =  arguments?["samplingRate"] as? Double ?? 8000.0
            //            playTone(digits: digits, samplingRate: samplingRate)
        }
        else if call.method == "playCallWaiting"
        {
            print("playCallWaiting")
            //            var soundID:SystemSoundID = 0
            //            //获取声音地址
            //            let path = Bundle.main.path(forResource: "Ring", ofType: "wav")
            //            print("@@@ playCallWaitingpath %s", path ?? "@default")
            //
            //            //地址转换
            //            let baseURL = NSURL(fileURLWithPath:path!)
            //            AudioServicesCreateSystemSoundID(baseURL, &soundID)
            //
            //            print("@@@ playCallWaitingsoundID %d", soundID)
            //            //添加音频结束时的回调
            //            let observer = UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque())
            //            AudioServicesAddSystemSoundCompletion(soundID, nil, nil, {
            //                (soundID, inClientData) -> Void in
            //                AudioServicesRemoveSystemSoundCompletion(soundID)
            //                AudioServicesDisposeSystemSoundID(soundID)
            //            }, observer)
            //            AudioServicesPlaySystemSound(soundID)

            let samplingRate =  arguments?["samplingRate"] as? Double ?? 8000.0
            playTone(digits: "X", samplingRate: samplingRate, markSpace: DTMF.long)
            result(nil)
        }
        else if call.method == "playCallAlert"
        {
            let samplingRate =  arguments?["samplingRate"] as? Double ?? 8000.0
            playTone(digits: "Y", samplingRate: samplingRate, markSpace: DTMF.short)
        }
        else if call.method == "playCallTerm"
        {
            let samplingRate =  arguments?["samplingRate"] as? Double ?? 8000.0
            playTone(digits: "Z", samplingRate: samplingRate, markSpace: DTMF.middle)
        }
        else if call.method == "pauseCallWaiting"
        {
            isSwitchSpeaker = !isSwitchSpeaker
            //正在切换
            if isSwitchSpeaker{
                if _player.isPlaying{
                    print("pauseCallWaiting isPlaying")
                    _player.stop()
                    print("pauseCallWaiting _player is stop")
                }
                if _engine.isRunning{
                     print("pauseCallWaiting isRunning")
                    _engine.detach(_player)
                    _engine.pause()
                    print("pauseCallWaiting _engine is pause")

                }
            }

            result(nil)
        }

    }

    func playTone(digits: String, samplingRate: Double, markSpace: MarkSpaceType = DTMF.motorola)
    {
        //正在切换听筒扬声器，导致q引擎isRunning返回false
        if isSwitchSpeaker{
            return
        }
        let _sampleRate = Float(samplingRate)

        if let tones = DTMF.tonesForString(digits) {
            let audioFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(_sampleRate), channels: 2, interleaved: false)!

            // fill up the buffer with some samples
            var allSamples = [Float]()
            for tone in tones {
                let samples = DTMF.generateDTMF(tone, markSpace: markSpace, sampleRate: _sampleRate)
                allSamples.append(contentsOf: samples)
            }

            let frameCount = AVAudioFrameCount(allSamples.count)
            let buffer = AVAudioPCMBuffer(pcmFormat: audioFormat, frameCapacity: frameCount)!

            buffer.frameLength = frameCount
            let channelMemory = buffer.floatChannelData!
            for channelIndex in 0 ..< Int(audioFormat.channelCount) {
                let frameMemory = channelMemory[channelIndex]
                memcpy(frameMemory, allSamples, Int(frameCount) * MemoryLayout<Float>.size)
            }

            if _player.isPlaying{
                print("playTone isPlaying")
                _player.stop()
            }else{
                print("playTone is not Playing")

            }
            if _engine.isRunning{
                _engine.detach(_player)
                _engine.pause()
            }

            _engine.attach(_player)
            _engine.connect(_player, to:_mixer, format:audioFormat)
            _engine.prepare()

            do {
                try _engine.start()
                _player.scheduleBuffer(buffer, at:nil,completionHandler:nil)

                //切换听筒外放被终止了
                if _engine.isRunning {
                    print("playTone isRunning")

                    _player.play()

                    print("playTone Running")

                }else{
                    print("playTone is not Running")

                }
            } catch let error as NSError {
                print("Engine start failed - \(error)")

            }

        }
    }
}
