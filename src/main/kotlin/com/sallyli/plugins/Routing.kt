package com.sallyli.plugins

import com.sallyli.services.GeminiLiveClient
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    val apiKey = System.getenv("GEMINI_API_KEY") ?: "testAPIKey"
    val geminiClient = GeminiLiveClient(apiKey)

    routing {
        get("/") {
            call.respondHtml {
                head {
                    title("Technical Interview Coach")
                    script(src = "https://cdn.tailwindcss.com") {}
                }
                body("bg-gray-100 flex items-center justify-center h-screen") {
                    div("bg-white p-8 rounded shadow-md w-full max-w-md text-center") {
                        h1("text-2xl font-bold mb-4") { +"Technical Interview Coach" }
                        p("text-gray-600 mb-6") { +"Press the button to start your mock interview." }
                        button(classes = "bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline") {
                            id = "start-btn"
                            +"Start Interview"
                        }
                        div("mt-6 p-4 bg-gray-50 rounded border h-48 overflow-y-auto text-left") {
                            id = "transcript"
                            +"Transcript will appear here..."
                        }
                        script {
                            unsafe {
                                raw("""
                                    const startBtn = document.getElementById('start-btn');
                                    const transcript = document.getElementById('transcript');
                                    let socket;
                                    let audioContext;
                                    let processor;
                                    let source;

                                    startBtn.onclick = async () => {
                                        if (socket) {
                                            socket.close();
                                            startBtn.innerText = "Start Interview";
                                            startBtn.classList.replace('bg-red-500', 'bg-blue-500');
                                            return;
                                        }

                                        socket = new WebSocket(`ws://${'$'}{window.location.host}/ws/interview`);
                                        socket.binaryType = 'arraybuffer';

                                        socket.onopen = async () => {
                                            startBtn.innerText = "Stop Interview";
                                            startBtn.classList.replace('bg-blue-500', 'bg-red-500');
                                            transcript.innerText = "";

                                            audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
                                            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
                                            source = audioContext.createMediaStreamSource(stream);
                                            processor = audioContext.createScriptProcessor(4096, 1, 1);

                                            processor.onaudioprocess = (e) => {
                                                const inputData = e.inputBuffer.getChannelData(0);
                                                const pcmData = new Int16Array(inputData.length);
                                                for (let i = 0; i < inputData.length; i++) {
                                                    pcmData[i] = Math.max(-1, Math.min(1, inputData[i])) * 0x7FFF;
                                                }
                                                if (socket.readyState === WebSocket.OPEN) {
                                                    socket.send(pcmData.buffer);
                                                }
                                            };

                                            source.connect(processor);
                                            processor.connect(audioContext.destination);
                                        };

                                        socket.onmessage = async (event) => {
                                            if (typeof event.data === 'string') {
                                                if (event.data === "INTERRUPTED") {
                                                    // Handle interruption
                                                } else {
                                                    const p = document.createElement('p');
                                                    p.innerText = event.data;
                                                    transcript.appendChild(p);
                                                    transcript.scrollTop = transcript.scrollHeight;
                                                }
                                            } else {
                                                // Play response audio (24kHz PCM)
                                                const audioData = new Int16Array(event.data);
                                                const floatData = new Float32Array(audioData.length);
                                                for (let i = 0; i < audioData.length; i++) {
                                                    floatData[i] = audioData[i] / 0x7FFF;
                                                }
                                                
                                                const responseContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 24000 });
                                                const buffer = responseContext.createBuffer(1, floatData.length, 24000);
                                                buffer.copyToChannel(floatData, 0);
                                                const responseSource = responseContext.createBufferSource();
                                                responseSource.buffer = buffer;
                                                responseSource.connect(responseContext.destination);
                                                responseSource.start();
                                            }
                                        };

                                        socket.onclose = () => {
                                            if (processor) processor.disconnect();
                                            if (source) source.disconnect();
                                            socket = null;
                                            startBtn.innerText = "Start Interview";
                                            startBtn.classList.replace('bg-red-500', 'bg-blue-500');
                                        };
                                    };
                                """)
                            }
                        }
                    }
                }
            }
        }

        webSocket("/ws/interview") {
            val inputChannel = Channel<Frame>()
            val outputChannel = Channel<Frame>()

            val geminiJob = launch {
                geminiClient.startSession(inputChannel, outputChannel)
            }

            val forwardInputJob = launch {
                for (frame in incoming) {
                    inputChannel.send(frame)
                }
            }

            val forwardOutputJob = launch {
                for (frame in outputChannel) {
                    send(frame)
                }
            }

            try {
                forwardInputJob.join()
                forwardOutputJob.join()
            } finally {
                geminiJob.cancel()
                inputChannel.close()
                outputChannel.close()
            }
        }
    }
}
