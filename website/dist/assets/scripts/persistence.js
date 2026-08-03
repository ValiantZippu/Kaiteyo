/**
 * Kaiteyo website — persistence.
 *
 * All site preferences live in localStorage under the "kaiteyo:" prefix,
 * mirroring how the desktop application persists preferences with DataStore.
 *
 * Keys:
 *   kaiteyo:theme   -> { baseMode, accent, glass }
 *   kaiteyo:nav     -> { position, mode, collapsed, autoHide, width, floating }
 *   kaiteyo:motion  -> "default" | "reduced"
 *   kaiteyo:ui      -> misc UI state
 */

const PREFIX = "kaiteyo:";

function serialize(value) {
  return typeof value === "string" ? value : JSON.stringify(value);
}

function deserialize(raw) {
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

export function getPreference(key, fallback = null) {
  try {
    const raw = window.localStorage.getItem(PREFIX + key);
    return raw === null ? fallback : deserialize(raw);
  } catch {
    return fallback;
  }
}

export function setPreference(key, value) {
  try {
    window.localStorage.setItem(PREFIX + key, serialize(value));
  } catch {
    /* storage unavailable (private mode, quota) — non-fatal */
  }
}

export function removePreference(key) {
  try {
    window.localStorage.removeItem(PREFIX + key);
  } catch {
    /* non-fatal */
  }
}
