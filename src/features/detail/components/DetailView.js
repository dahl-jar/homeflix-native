import { ScrollView, StyleSheet, View } from 'react-native';

import { colors, spacing } from '../../../theme/tokens.js';

import { CastRow } from './CastRow.js';
import { DetailActions } from './DetailActions.js';
import { DetailChips } from './DetailChips.js';
import { DetailDescription } from './DetailDescription.js';
import { DetailHero } from './DetailHero.js';
import { EpisodeSection } from './EpisodeSection.js';
import { RecommendationRow } from './RecommendationRow.js';

const BOTTOM_CLEARANCE = 60;

export function DetailView({ data, onBack, serverUrl, width }) {
    return (
        <ScrollView
            style={styles.screen}
            contentContainerStyle={{ paddingBottom: BOTTOM_CLEARANCE }}
            showsVerticalScrollIndicator={false}
        >
            <DetailHero item={data.item} onBack={onBack} serverUrl={serverUrl} width={width} />
            <View style={styles.body}>
                <DetailChips item={data.item} />
                <DetailActions item={data.item} sources={data.sources} />
                <DetailDescription item={data.item} />
                <EpisodeSection
                    episodes={data.episodes}
                    onSelectSeason={data.selectSeason}
                    seasonIndex={data.seasonIndex}
                    seasons={data.seasons}
                    serverUrl={serverUrl}
                />
                <RecommendationRow items={data.similar} serverUrl={serverUrl} />
                <CastRow people={data.item.People} serverUrl={serverUrl} />
            </View>
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    screen: {
        flex: 1,
        backgroundColor: colors.bg
    },
    body: {
        paddingHorizontal: spacing.screen
    }
});
