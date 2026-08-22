import assert from 'node:assert/strict';
import { test } from 'node:test';

import { sanitizeTelemetryFields } from '../telemetrySanitizer.js';

test('should remove sensitive and unsupported telemetry fields', () => {
    const fields = sanitizeTelemetryFields({
        event: 'source_failed',
        pipelineId: 'pipeline-one',
        itemId: 'item-one',
        mediaSourceId: 'source-one',
        sourceName: 'Movie 1080p',
        videoUrl: 'https://provider.example/video?token=secret',
        path: '/provider/secret',
        authorization: 'Token="secret"',
        accessToken: 'secret',
        request: { headers: { Authorization: 'secret' } },
        unknownField: 'ignored'
    });

    assert.deepEqual(fields, {
        event: 'source_failed',
        pipelineId: 'pipeline-one',
        itemId: 'item-one',
        mediaSourceId: 'source-one',
        sourceName: 'Movie 1080p'
    });
});

test('should redact urls and credentials inside allowed text values', () => {
    const fields = sanitizeTelemetryFields({
        event: 'source_failed',
        pipelineId: 'pipeline-one',
        reason: 'fetch https://provider.example/video?api_key=secret failed',
        errorMessage: 'Authorization: MediaBrowser Token="secret"'
    });

    assert.equal(fields.reason, 'fetch <redacted> failed');
    assert.equal(fields.errorMessage, 'Authorization: <redacted>');
    assert.doesNotMatch(JSON.stringify(fields), /provider|secret|api_key/);
});

test('should normalize control characters and server field limits', () => {
    const fields = sanitizeTelemetryFields({
        event: 'source_failed',
        pipelineId: 'pipeline-one',
        itemName: `Movie\n${'x'.repeat(400)}`,
        sourceName: `Source\t${'y'.repeat(700)}`
    });

    assert.equal(fields.itemName.length, 256);
    assert.equal(fields.sourceName.length, 512);
    assert.doesNotMatch(fields.itemName, /[\n\t]/);
});
