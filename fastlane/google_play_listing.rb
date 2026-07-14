# frozen_string_literal: true

module GooglePlayListing
  def self.latest_version_code(version_codes, track:)
    candidates = Array(version_codes).compact.select(&:positive?)
    return candidates.max unless candidates.empty?

    raise ArgumentError,
          "No existing version code found on Google Play track '#{track}'. " \
          "Upload a release to that track first or set ANDROID_PLAY_LISTING_TRACK to a populated track."
  end
end
