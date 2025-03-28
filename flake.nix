{
  description = "Minecraft Forge Mod Development Environment on NixOS";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      pkgs = import nixpkgs { system = "x86_64-linux"; };
    in
    {
      devShells.x86_64-linux.default = pkgs.mkShell {
        name = "minecraft-forge-dev";

        buildInputs = with pkgs; [
          jdk17
          gradle
          git
          mesa
          libglvnd
          glfw
          xorg.libX11
          xorg.libXcursor
          xorg.libXrandr
          xorg.libXxf86vm
          libpulseaudio
          alsa-lib
          vulkan-loader
        ];

        shellHook = ''
          export LD_LIBRARY_PATH=${
            pkgs.lib.makeLibraryPath [
              pkgs.mesa
              pkgs.libglvnd
              pkgs.glfw
              pkgs.xorg.libX11
              pkgs.xorg.libXcursor
              pkgs.xorg.libXrandr
              pkgs.xorg.libXxf86vm
              pkgs.libpulseaudio
              pkgs.alsa-lib
              pkgs.vulkan-loader
            ]
          }:$LD_LIBRARY_PATH

          export LIBGL_DEBUG=verbose
          echo "Minecraft Forge modding environment ready!"
        '';
      };
    };
}
