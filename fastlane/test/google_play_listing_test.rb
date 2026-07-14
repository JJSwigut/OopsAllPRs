# frozen_string_literal: true

require "minitest/autorun"
require_relative "../google_play_listing"

FASTLANE_LANES = {}

module UI
  def self.message(_message); end

  def self.user_error!(message)
    raise message
  end
end

def opt_out_usage; end

def default_platform(_platform); end

def desc(_description); end

def platform(name)
  previous_platform = @test_platform
  @test_platform = name
  yield
ensure
  @test_platform = previous_platform
end

def lane(name, &block)
  FASTLANE_LANES[[@test_platform, name]] = block
end

load File.expand_path("../Fastfile", __dir__)

class GooglePlayListingTest < Minitest::Test
  def test_selects_highest_existing_version_code
    assert_equal 42, GooglePlayListing.latest_version_code([17, 42, 31], track: "internal")
  end

  def test_ignores_nil_and_non_positive_version_codes
    assert_equal 9, GooglePlayListing.latest_version_code([nil, 0, -1, 9], track: "internal")
  end

  def test_requires_a_populated_track
    error = assert_raises(ArgumentError) do
      GooglePlayListing.latest_version_code([], track: "internal")
    end

    assert_includes error.message, "No existing version code found on Google Play track 'internal'"
  end

  def test_listing_lane_targets_existing_release_without_release_upload_paths
    track_requests = []
    upload_options = nil

    define_singleton_method(:google_play_track_version_codes) do |**options|
      track_requests << options
      [17, 42, 31]
    end
    define_singleton_method(:prepare_android_listing_assets) { "/tmp/google-play-listing" }
    define_singleton_method(:upload_to_play_store) { |**options| upload_options = options }

    with_environment(
      "ANDROID_PACKAGE_NAME" => "com.example.app",
      "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" => "test-credential"
    ) do
      instance_exec(&FASTLANE_LANES.fetch([:android, :listing]))
    end

    assert_equal(
      [{ package_name: "com.example.app", json_key_data: "test-credential", track: "internal" }],
      track_requests
    )
    assert_equal "internal", upload_options.fetch(:track)
    assert_equal 42, upload_options.fetch(:version_code)
    assert_equal true, upload_options.fetch(:skip_upload_apk)
    assert_equal true, upload_options.fetch(:skip_upload_aab)
    assert_equal true, upload_options.fetch(:skip_upload_changelogs)
    assert_equal true, upload_options.fetch(:validate_only)
    assert_equal true, upload_options.fetch(:changes_not_sent_for_review)
    assert_equal false, upload_options.fetch(:rescue_changes_not_sent_for_review)
    refute upload_options.key?(:apk)
    refute upload_options.key?(:aab)
    refute upload_options.key?(:track_promote_to)
    refute upload_options.key?(:rollout)
    refute upload_options.key?(:release_status)
  end

  private

  def with_environment(values)
    previous = values.to_h { |key, _value| [key, ENV[key]] }
    values.each { |key, value| ENV[key] = value }
    yield
  ensure
    previous.each { |key, value| value.nil? ? ENV.delete(key) : ENV[key] = value }
  end
end
