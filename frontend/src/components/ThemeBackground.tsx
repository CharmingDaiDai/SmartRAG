import { useEffect, useState } from "react";
import Particles, { initParticlesEngine } from "@tsparticles/react";
import { loadSlim } from "@tsparticles/slim";
import { useAppStore } from "../store/useAppStore";

export default function ThemeBackground() {
  const [init, setInit] = useState(false);
  const { uiStyle } = useAppStore();

  useEffect(() => {
    initParticlesEngine(async (engine) => {
      await loadSlim(engine);
    }).then(() => {
      setInit(true);
    });
  }, []);

  if (!init) return null;

  if (uiStyle === "tech") {
    return (
      <Particles
        id="tsparticles-tech"
        options={{
          background: {
            color: {
              value: "transparent",
            },
          },
          fpsLimit: 60,
          interactivity: {
            events: {
              onHover: {
                enable: true,
                mode: "grab",
              },
            },
            modes: {
              grab: {
                distance: 140,
                links: {
                  opacity: 1,
                },
              },
            },
          },
          particles: {
            color: {
              value: "#00ffcc",
            },
            links: {
              color: "#00ffcc",
              distance: 150,
              enable: true,
              opacity: 0.2,
              width: 1,
            },
            move: {
              direction: "none",
              enable: true,
              outModes: {
                default: "bounce",
              },
              random: false,
              speed: 1,
              straight: false,
            },
            number: {
              density: {
                enable: true,
                width: 800,
                height: 800,
              },
              value: 60,
            },
            opacity: {
              value: 0.3,
            },
            shape: {
              type: "circle",
            },
            size: {
              value: { min: 1, max: 3 },
            },
          },
          detectRetina: true,
        }}
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          zIndex: 0,
          pointerEvents: "none",
        }}
      />
    );
  }

  if (uiStyle === "playful") {
    return (
      <Particles
        id="tsparticles-playful"
        options={{
          background: {
            color: {
              value: "transparent",
            },
          },
          fpsLimit: 60,
          particles: {
            color: {
              value: ["#ff4757", "#2ed573", "#1e90ff", "#ffa502", "#ff6b81"],
            },
            move: {
              direction: "top",
              enable: true,
              outModes: {
                default: "out",
              },
              random: true,
              speed: 2,
              straight: false,
            },
            number: {
              density: {
                enable: true,
                width: 800,
                height: 800,
              },
              value: 30,
            },
            opacity: {
              value: 0.6,
            },
            shape: {
              type: ["circle", "square", "triangle"],
            },
            size: {
              value: { min: 10, max: 20 },
            },
          },
          detectRetina: true,
        }}
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          zIndex: 0,
          pointerEvents: "none",
        }}
      />
    );
  }

  return null;
}
