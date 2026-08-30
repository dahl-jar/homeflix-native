import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    pickerIndexFromOffset,
    pickerInsetForViewport,
    pickerOffsetForIndex,
    selectedPickerIndex
} from '../pickerModel.js';

test('should map centered offset to row', () => {
    const offset = pickerOffsetForIndex(2);

    assert.equal(pickerIndexFromOffset(offset, 4), 2);
});

test('should center row in viewport', () => {
    assert.equal(pickerInsetForViewport(300), 127.5);
});

test('should clamp short viewport inset', () => {
    assert.equal(pickerInsetForViewport(40), 0);
});

test('should clamp offset before first row', () => {
    assert.equal(pickerIndexFromOffset(-500, 4), 0);
});

test('should clamp offset after last row', () => {
    assert.equal(pickerIndexFromOffset(500, 4), 3);
});

test('should find selected row', () => {
    const entries = [{ key: 'one' }, { key: 'two' }];

    assert.equal(selectedPickerIndex(entries, ({ key }) => key === 'two'), 1);
});

test('should default to first row', () => {
    assert.equal(selectedPickerIndex([{ key: 'one' }], () => false), 0);
});

test('should reject offset for empty picker', () => {
    assert.equal(pickerIndexFromOffset(0, 0), -1);
});

test('should reject selection for empty picker', () => {
    assert.equal(selectedPickerIndex([], () => false), -1);
});
