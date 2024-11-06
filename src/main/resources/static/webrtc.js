const localVideo = document.getElementById('localVideo');
const remoteVideos = document.getElementById('remoteVideos');
let localStream;
let peerConnections = {};

// Create or retrieve a unique peer ID
const peerId = sessionStorage.getItem('peerId') || 'peer-' + Math.floor(Math.random() * 10000);
sessionStorage.setItem('peerId', peerId);
document.getElementById('peerId').textContent = `Your Peer ID: ${peerId}`;

// WebSocket signaling server
const LOCAL_IP_ADDRESS = "10.24.13.122";
const signalingServer = new WebSocket(`ws://${LOCAL_IP_ADDRESS}:8080/ws`);

signalingServer.onmessage = function (message) {
    const data = JSON.parse(message.data);

    switch (data.type) {
        case "user-joined":
            // When a new peer joins, create an offer for that peer
            if (data.peerId !== peerId) {
                createAndSendOffer(data.peerId);
            }
            break;
        case "user-left":
            // When a peer disconnects
            if (peerConnections[data.peerId]) {
                peerConnections[data.peerId].close();
                delete peerConnections[data.peerId];
                // Remove the video element of the disconnected peer
                const videoContainer = document.querySelector(`[data-peer-id="${data.peerId}"]`);
                if (videoContainer) {
                    videoContainer.remove();
                }
            }
            break;
        case "offer":
            handleOffer(data);
            break;
        case "answer":
            handleAnswer(data);
            break;
        case "candidate":
            handleCandidate(data);
            break;
        default:
            break;
    }
};

// Start the local video stream
async function startStream() {
    try {
        localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        localVideo.srcObject = localStream;
        Object.values(peerConnections).forEach(pc => {
            localStream.getTracks().forEach(track => pc.addTrack(track, localStream));
        });
        // Notify server that the peer is ready
        signalingServer.send(JSON.stringify({
            type: "join",
            peerId: peerId
        }));
    } catch (error) {
        console.error("Error accessing media devices:", error);
    }
}

// Stop the local video stream
function stopStream() {
    if (localStream) {
        localStream.getTracks().forEach(track => track.stop());
        localVideo.srcObject = null;
        // Close all peer connections
        Object.values(peerConnections).forEach(pc => pc.close());
        peerConnections = {};
        // Remove all remote videos
        remoteVideos.innerHTML = '';
        // Notify the server
        signalingServer.send(JSON.stringify({
            type: "leave",
            peerId: peerId
        }));
    }
}

async function createAndSendOffer(targetPeerId) {
    const peerConnection = createPeerConnection(targetPeerId);
    try {
        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);
        signalingServer.send(JSON.stringify({
            type: "offer",
            offer: offer,
            to: targetPeerId,
            from: peerId
        }));
    } catch (error) {
        console.error("Error creating offer:", error);
    }
}

async function handleOffer(data) {
    const peerConnection = createPeerConnection(data.from);
    try {
        await peerConnection.setRemoteDescription(new RTCSessionDescription(data.offer));
        const answer = await peerConnection.createAnswer();
        await peerConnection.setLocalDescription(answer);
        signalingServer.send(JSON.stringify({
            type: "answer",
            answer: answer,
            to: data.from,
            from: peerId
        }));
    } catch (error) {
        console.error("Error handling offer:", error);
    }
}

function handleAnswer(data) {
    if (peerConnections[data.from]) {
        peerConnections[data.from].setRemoteDescription(new RTCSessionDescription(data.answer))
            .catch(error => console.error("Error setting remote description:", error));
    }
}

function handleCandidate(data) {
    if (peerConnections[data.from]) {
        peerConnections[data.from].addIceCandidate(new RTCIceCandidate(data.candidate))
            .catch(error => console.error("Error adding ICE candidate:", error));
    }
}

function createPeerConnection(id) {
    // Use STUN servers to assist with NAT traversal
    const configuration = {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' }
        ]
    };

    const peerConnection = new RTCPeerConnection(configuration);
    peerConnections[id] = peerConnection;

    peerConnection.ontrack = event => {
        // Check if a video container for this peer already exists
        let videoContainer = document.querySelector(`[data-peer-id="${id}"]`);
        if (!videoContainer) {
            videoContainer = document.createElement('div');
            videoContainer.className = 'videoContainer';
            videoContainer.setAttribute('data-peer-id', id);

            const label = document.createElement('p');
            label.textContent = `Peer ID: ${id}`;
            label.style.textAlign = 'center';

            const video = document.createElement('video');
            video.srcObject = event.streams[0];
            video.autoplay = true;
            video.playsinline = true;

            videoContainer.appendChild(label);
            videoContainer.appendChild(video);
            remoteVideos.appendChild(videoContainer);
        } else {
            // If container exists, update the video stream
            const video = videoContainer.querySelector('video');
            video.srcObject = event.streams[0];
        }
    };

    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            signalingServer.send(JSON.stringify({
                type: "candidate",
                candidate: event.candidate,
                to: id,
                from: peerId
            }));
        }
    };

    // Add local tracks to peer connection
    if (localStream) {
        localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
    }

    return peerConnection;
}

// Handle window close event
window.onbeforeunload = function () {
    stopStream();
};