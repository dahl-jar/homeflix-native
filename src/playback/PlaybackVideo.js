import { VideoView } from 'expo-video';
import { StyleSheet } from 'react-native';

export function PlaybackVideo({ contentFit, pipeline, player }) {
    if (!pipeline.videoVisible) return null;
    return (
        <VideoView
            allowsPictureInPicture={false}
            contentFit={contentFit}
            nativeControls={false}
            player={player}
            style={StyleSheet.absoluteFill}
        />
    );
}
