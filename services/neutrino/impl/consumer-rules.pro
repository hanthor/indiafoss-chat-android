# DefaultNeutrinoService resolves the FFI's set_discoverable reflectively (see
# ReflectiveDiscoverableBinding): the pinned .aar may not carry it, so nothing
# references it statically and R8 would otherwise strip it from a build whose
# bindings do have it. Keep the uniffi facade's static so the runtime lookup
# finds it under its real name.
-keepclassmembers class io.element.neutrino.ble.Neutrino_bleKt {
    public static void setDiscoverable(boolean);
}
