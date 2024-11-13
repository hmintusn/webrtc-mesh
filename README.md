# Video Conferencing

## Members:
1. Hoàng Minh Tuấn - B21DCVT444
2. Nguyễn Hoàng Long - B21DCCN497
3. Nguyễn Doãn Hoàng Giang - B21DCAT003

## Description:
- (Focus on) video conferencing solution to handle online meetings for 1-to-1 and group interactions. 
- Real-time messaging for both individual and group chats.

## Technology:
- Java (Spring Boot)
- WebSocket
- WebRTC with Mesh architecture

## WebRTC Background Research

### 1. WebRTC (Web Real-Time Communication)
Audio and video communication system designed to work inside web pages.

#### Core Components
WebRTC consists of three main parts:
1. Signalling
2. Connecting
3. Communicating

#### Key Features
* **P2P Protocol**: Direct peer-to-peer communication with no need for traffic routing through a central server
* **Signaling Server**: 
  * Used only for initial channel setup
  * Determines media format and peer communication preferences
  * Facilitates exchange of:
    * Session keys
    * Error messages
    * Media metadata
    * Codecs
    * Bandwidth information
    * Public IP addresses and ports

#### Network Components

##### NAT and ICE Framework
* **NAT (Network Address Translation)**:
  * Maps private addresses inside a local network to public IP addresses
  * Enables transmission onto the internet

* **ICE (Interactive Connectivity Establishment)**:
  * Framework that tests all connections in parallel
  * Selects the most efficient connection path
  * Two primary connection types:

  1. **STUN (Session Traversal Utilities for NAT) Servers**:
     * Detects NAT type in use
     * Provides public IP address for peer communication
     * Handles "Symmetric NAT" cases where routers only accept previously connected peers
     * ![image](https://github.com/user-attachments/assets/5d5c5f8d-d397-4701-ace7-7165eee50e21)

  2. **TURN (Traversal Using Relays Around NATs)**:
     * Functions as relay servers for failed P2P connections
     * Maintains media relay between WebRTC peers
     * ![image](https://github.com/user-attachments/assets/0c23dcfe-b577-4b45-a34f-4fc055e4a4f9)


#### Technical Components

##### Video Codec (VP9)
* Compresses and decompresses audio/video
* Key features:
  * Packet loss concealment
  * Noisy image cleanup
  * Cross-platform capture and playback

##### JavaScript APIs
Three main APIs handle core functionality:

1. **MediaStream**:
   * Captures audio and video from user devices
   * Allows setting of constraints (frame rate, resolution)

2. **RTCPeerConnection**:
   * Manages real-time audio/video transmission
   * Handles connection lifecycle (connect, maintain, close)

3. **RTCDataChannel**:
   * Transmits arbitrary data
   * Integrated with RTCPeerConnection
   * Provides security and congestion control

### 2. WebRTC Mesh Architecture

#### Advantages
1. **Cost Efficiency**:
   * No media servers required
   * Reduced bandwidth usage
   * Lower overall costs

2. **Privacy**:
   * Direct media flow between users
   * Service provider has no access to data
   * Enhanced privacy protection

#### Disadvantages
1. **Bandwidth and CPU Limitations**:
   * Multiple media streams per device
   * Can overwhelm system resources
   * Performance degradation with increased load

2. **Scalability Issues**:
   * Less effective for 3+ user calls
   * Media server-based solutions preferred for larger groups
   * Performance and user experience challenges
     
## Future Development
### SFU (Selective Forwarding Unit)

A **video conferencing architecture** that optimizes data transmission between server and endpoints through the following process:

#### Data Flow Process
1. Server receives incoming video streams from all endpoints
2. Server distributes uncompressed video streams to each endpoint
3. Endpoints merge incoming video streams from other participants

#### Key Characteristics
* Stream centralization through server-based distribution
* Each participant:
  * Sends single outgoing signal
  * Receives multiple streams from other users via server
  * Reduces individual endpoint processing load
