const MINIMUM_STAGE_WIDTH = 64;
const MAXIMUM_STAGE_WIDTH = 140;

export function pipelineStageLayout(viewportWidth, stageCount) {
    if (stageCount <= 0 || viewportWidth <= 0) {
        return { centered: true, stageWidth: MAXIMUM_STAGE_WIDTH };
    }
    const stageWidth = Math.min(
        MAXIMUM_STAGE_WIDTH,
        Math.max(MINIMUM_STAGE_WIDTH, viewportWidth / stageCount)
    );
    return {
        centered: stageWidth * stageCount <= viewportWidth,
        stageWidth
    };
}
