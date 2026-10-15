import { useEffect, useMemo, useRef, useState } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import type { CountryInfo } from '../../services/api/liveTvApi';
import { clsx } from 'clsx';
import centroids from '../../data/liveTvCountryCentroids.json';

interface EarthGlobeProps {
  selectedCountry: string;
  countries: CountryInfo[];
  onCountryChange: (code: string) => void;
}

type Point = { lat: number; lon: number; name?: string };
type Vec3 = { x: number; y: number; z: number };
type Quat = { x: number; y: number; z: number; w: number };
type GeoFeature = {
  code: string;
  iso3: string;
  name: string;
  geometry: { type: 'Polygon' | 'MultiPolygon'; coordinates: number[][][] | number[][][][] };
};
type GeoCollection = { type: 'FeatureCollection'; features: GeoFeature[] };
type HitRegion = { code: string; path: Path2D; depth: number };

const countryPoints = centroids as Record<string, Point>;

function quatNormalize(q: Quat): Quat {
  const m = Math.hypot(q.x, q.y, q.z, q.w) || 1;
  return { x: q.x / m, y: q.y / m, z: q.z / m, w: q.w / m };
}

function quatMultiplyRaw(a: Quat, b: Quat): Quat {
  return {
    w: a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
    x: a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
    y: a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
    z: a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
  };
}

function quatMultiply(a: Quat, b: Quat): Quat {
  return quatNormalize(quatMultiplyRaw(a, b));
}

function axisAngle(axis: Vec3, radians: number): Quat {
  const half = radians / 2;
  const s = Math.sin(half);
  return { x: axis.x * s, y: axis.y * s, z: axis.z * s, w: Math.cos(half) };
}

function rotateVector(v: Vec3, q: Quat): Vec3 {
  const qv = { x: v.x, y: v.y, z: v.z, w: 0 };
  const qi = { x: -q.x, y: -q.y, z: -q.z, w: q.w };
  const r = quatMultiplyRaw(quatMultiplyRaw(q, qv), qi);
  return { x: r.x, y: r.y, z: r.z };
}

function latLonToVector(lat: number, lon: number): Vec3 {
  const phi = (lat * Math.PI) / 180;
  const lambda = (lon * Math.PI) / 180;
  return {
    x: Math.cos(phi) * Math.sin(lambda),
    y: Math.sin(phi),
    z: Math.cos(phi) * Math.cos(lambda),
  };
}

function shortestLongitude(lon: number) {
  let value = lon;
  while (value > 180) value -= 360;
  while (value < -180) value += 360;
  return value;
}

function channelHeat(count: number, max: number) {
  if (!count || !max) return 'rgba(44,61,73,.68)';
  const t = Math.max(0, Math.min(1, Math.log1p(count) / Math.log1p(max)));
  const r = Math.round(37 + 180 * t);
  const g = Math.round(110 + 105 * t);
  const b = Math.round(145 - 75 * t);
  return `rgba(${r},${g},${b},${0.58 + 0.32 * t})`;
}

function geometryRings(feature: GeoFeature): number[][][] {
  if (feature.geometry.type === 'Polygon') return feature.geometry.coordinates as number[][][];
  return (feature.geometry.coordinates as number[][][][]).flat();
}

/**
 * V12.1 Earth:
 * - Real country polygons projected onto a spherical surface.
 * - Quaternion-based free rotation, including pitch/yaw and roll.
 * - Antarctica is reachable by vertical dragging; there is no longitude-only lock.
 * - Countries are individually hoverable/clickable.
 * - Country fill is a live heatmap based on the channel count returned by the backend.
 * - Missing tiny-island polygons still get clickable centroid hotspots from the IPTV country catalogue.
 * - No automatic rotation.
 */
export function EarthGlobe({ selectedCountry, countries, onCountryChange }: EarthGlobeProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animationRef = useRef<number | null>(null);
  const rotationRef = useRef<Quat>(axisAngle({ x: 0, y: 1, z: 0 }, -0.32));
  const draggingRef = useRef(false);
  const movedRef = useRef(false);
  const pointerRef = useRef({ x: 0, y: 0 });
  const dragModeRef = useRef<'trackball' | 'roll'>('trackball');
  const hitRegionsRef = useRef<HitRegion[]>([]);
  const geoRef = useRef<GeoCollection | null>(null);
  const [geoReady, setGeoReady] = useState(false);
  const [hoveredCountry, setHoveredCountry] = useState<string>('');

  const sortedCountries = useMemo(
    () => [...countries].sort((a, b) => a.name.localeCompare(b.name)),
    [countries],
  );
  const countryByCode = useMemo(() => {
    const map = new Map<string, CountryInfo>();
    countries.forEach(country => map.set(country.code.toUpperCase(), country));
    return map;
  }, [countries]);
  const maxChannels = useMemo(
    () => Math.max(1, ...countries.map(country => country.channelCount || 0)),
    [countries],
  );

  useEffect(() => {
    let disposed = false;
    fetch('/live-tv/world-countries-lowres.geojson')
      .then(response => {
        if (!response.ok) throw new Error(`country geometry HTTP ${response.status}`);
        return response.json() as Promise<GeoCollection>;
      })
      .then(data => {
        if (disposed) return;
        geoRef.current = data;
        setGeoReady(true);
      })
      .catch(() => {
        if (!disposed) setGeoReady(false);
      });
    return () => {
      disposed = true;
    };
  }, []);

  const applyTrackballRotation = (dx: number, dy: number) => {
    const speed = 0.0062;
    const yaw = axisAngle({ x: 0, y: 1, z: 0 }, dx * speed);
    const pitch = axisAngle({ x: 1, y: 0, z: 0 }, dy * speed);
    rotationRef.current = quatMultiply(pitch, quatMultiply(yaw, rotationRef.current));
  };

  const applyRollRotation = (dx: number) => {
    rotationRef.current = quatMultiply(
      axisAngle({ x: 0, y: 0, z: 1 }, dx * 0.008),
      rotationRef.current,
    );
  };

  const renderGlobeRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: true });
    if (!ctx) return;

    let disposed = false;
    const draw = () => {
      if (disposed) return;
      const rect = canvas.getBoundingClientRect();
      const cssSize = Math.max(260, Math.floor(Math.min(rect.width, rect.height || rect.width)));
      const dpr = Math.min(window.devicePixelRatio || 1, 1.35);
      const renderSize = Math.min(Math.floor(cssSize * dpr), 980);
      if (canvas.width !== renderSize || canvas.height !== renderSize) {
        canvas.width = renderSize;
        canvas.height = renderSize;
      }

      const radius = renderSize * 0.455;
      const cx = renderSize / 2;
      const cy = renderSize / 2;
      const q = rotationRef.current;
      const hits: HitRegion[] = [];

      ctx.clearRect(0, 0, renderSize, renderSize);

      // Fast radial sphere shading. The old implementation shaded every pixel
      // on every animation frame; that made the globe CPU-heavy and delayed the
      // catalogue from becoming visible. The Earth is now rendered only when
      // its state changes (drag/hover/resize/filter), with the same visual depth.
      const sphereGradient = ctx.createRadialGradient(
        cx - radius * 0.34,
        cy - radius * 0.38,
        radius * 0.06,
        cx,
        cy,
        radius * 1.08,
      );
      sphereGradient.addColorStop(0, 'rgba(36,92,128,.98)');
      sphereGradient.addColorStop(0.48, 'rgba(20,57,83,.98)');
      sphereGradient.addColorStop(0.82, 'rgba(10,31,49,.98)');
      sphereGradient.addColorStop(1, 'rgba(2,8,14,.98)');
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.fillStyle = sphereGradient;
      ctx.fill();

      ctx.save();
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.clip();

      const geometry = geoRef.current;
      if (geometry) {
        for (const feature of geometry.features) {
          const countryCode = feature.code.toUpperCase();
          const country = countryByCode.get(countryCode);
          const fill = country ? channelHeat(country.channelCount, maxChannels) : 'rgba(45,65,78,.58)';
          const isSelected = selectedCountry.toUpperCase() === countryCode;
          const isHovered = hoveredCountry === countryCode;
          const rings = geometryRings(feature);
          let frontPath: Path2D | null = null;

          for (const ring of rings) {
            if (ring.length < 3) continue;
            const projected: Array<{ x: number; y: number; z: number }> = [];
            for (const coord of ring) {
              const rotated = rotateVector(latLonToVector(coord[1], shortestLongitude(coord[0])), q);
              projected.push({
                x: cx + rotated.x * radius,
                y: cy - rotated.y * radius,
                z: rotated.z,
              });
            }

            // Full coherent path prevents the large purple/black rectangular
            // artefacts caused by closing paths after skipping back-facing points.
            const path = new Path2D();
            path.moveTo(projected[0].x, projected[0].y);
            let canFill = true;
            for (let i = 1; i < projected.length; i += 1) {
              const prev = projected[i - 1];
              const point = projected[i];
              const jump = Math.hypot(point.x - prev.x, point.y - prev.y);
              if (jump > radius * 1.15) {
                canFill = false;
                path.moveTo(point.x, point.y);
              } else {
                path.lineTo(point.x, point.y);
              }
            }
            const closingJump = Math.hypot(projected[projected.length - 1].x - projected[0].x, projected[projected.length - 1].y - projected[0].y);
            if (closingJump > radius * 1.15) canFill = false;
            path.closePath();

            const ringDepth = projected.reduce((sum, point) => sum + point.z, 0) / projected.length;
            if (ringDepth > -0.02) {
              if (!frontPath) frontPath = path;
              ctx.fillStyle = isSelected ? 'rgba(126,232,108,.96)' : isHovered ? 'rgba(147,229,255,.96)' : fill;
              ctx.strokeStyle = isSelected ? 'rgba(218,255,203,.96)' : isHovered ? 'rgba(167,239,255,.96)' : 'rgba(104,196,223,.58)';
              ctx.lineWidth = isSelected ? Math.max(1.5, renderSize / 260) : Math.max(.55, renderSize / 900);
              if (canFill) ctx.fill(path);
              ctx.stroke(path);
              hits.push({ code: countryCode, path, depth: ringDepth });
            }
          }

          // Tiny islands may not have a visible low-resolution polygon. Keep the
          // IPTV country catalogue clickable through its known centroid.
          if (!frontPath) {
            const point = countryPoints[countryCode];
            if (point) {
              const rotated = rotateVector(latLonToVector(point.lat, point.lon), q);
              if (rotated.z > 0.08) {
                const px = rotated.x * radius;
                const py = -rotated.y * radius;
                const dot = new Path2D();
                dot.arc(cx + px, cy + py, Math.max(4, renderSize / 120), 0, Math.PI * 2);
                ctx.fillStyle = isSelected ? 'rgba(126,232,108,.98)' : isHovered ? 'rgba(147,229,255,.98)' : fill;
                ctx.fill(dot);
                ctx.strokeStyle = 'rgba(111,213,239,.82)';
                ctx.lineWidth = 1;
                ctx.stroke(dot);
                hits.push({ code: countryCode, path: dot, depth: rotated.z });
              }
            }
          }
        }
      }

      // Thin orbital rim and selected-country radar pulse.
      ctx.restore();
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.strokeStyle = 'rgba(99,211,255,.62)';
      ctx.lineWidth = Math.max(1.5, renderSize / 380);
      ctx.shadowColor = 'rgba(45,181,238,.28)';
      ctx.shadowBlur = renderSize / 28;
      ctx.stroke();
      ctx.shadowBlur = 0;

      if (selectedCountry) {
        const point = countryPoints[selectedCountry.toUpperCase()];
        if (point) {
          const rotated = rotateVector(latLonToVector(point.lat, point.lon), q);
          if (rotated.z > 0.02) {
            const px = rotated.x * radius;
            const py = -rotated.y * radius;
            ctx.beginPath();
            ctx.arc(cx + px, cy + py, renderSize / 92, 0, Math.PI * 2);
            ctx.strokeStyle = 'rgba(183,255,156,.9)';
            ctx.lineWidth = Math.max(1, renderSize / 450);
            ctx.stroke();
          }
        }
      }

      hitRegionsRef.current = hits;
    };

    renderGlobeRef.current = draw;
    const resizeObserver = new ResizeObserver(() => draw());
    resizeObserver.observe(canvas);
    draw();

    return () => {
      disposed = true;
      resizeObserver.disconnect();
      if (renderGlobeRef.current === draw) renderGlobeRef.current = null;
    };
  }, [countryByCode, geoReady, hoveredCountry, maxChannels, selectedCountry]);

  const hitCountryAt = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return '';
    const rect = canvas.getBoundingClientRect();
    const dpr = canvas.width / Math.max(1, rect.width);
    const x = (event.clientX - rect.left) * dpr;
    const y = (event.clientY - rect.top) * dpr;
    const ordered = [...hitRegionsRef.current].sort((a, b) => b.depth - a.depth);
    for (const region of ordered) {
      const ctx = canvas.getContext('2d');
      if (ctx?.isPointInPath(region.path, x, y)) return region.code;
    }
    return '';
  };

  const handlePointerDown = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    if (event.button !== 0 && event.button !== 2) return;
    draggingRef.current = true;
    movedRef.current = false;
    pointerRef.current = { x: event.clientX, y: event.clientY };
    dragModeRef.current = event.button === 2 || event.shiftKey ? 'roll' : 'trackball';
    event.currentTarget.setPointerCapture(event.pointerId);
    event.preventDefault();
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    if (draggingRef.current) {
      const dx = event.clientX - pointerRef.current.x;
      const dy = event.clientY - pointerRef.current.y;
      pointerRef.current = { x: event.clientX, y: event.clientY };
      if (Math.abs(dx) + Math.abs(dy) > 2) movedRef.current = true;
      if (dragModeRef.current === 'roll') applyRollRotation(dx + dy * 0.35);
      else applyTrackballRotation(dx, dy);
      renderGlobeRef.current?.();
      return;
    }
    const code = hitCountryAt(event);
    if (code !== hoveredCountry) setHoveredCountry(code);
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    if (!draggingRef.current) return;
    draggingRef.current = false;
    if (!movedRef.current && event.button === 0) {
      const code = hitCountryAt(event);
      if (code && countryByCode.has(code)) onCountryChange(code);
    }
  };

  const handleContextMenu = (event: ReactPointerEvent<HTMLCanvasElement>) => event.preventDefault();

  return (
    <div className="live-tv-earth-wrap">
      <div className="live-tv-earth-halo" />
      <canvas
        ref={canvasRef}
        className={clsx('live-tv-earth-canvas', hoveredCountry && 'country-hover')}
        aria-label="Interactive 3D Earth. Drag freely to rotate. Click a country to select it. Shift-drag or right-drag rotates the globe around its Z axis."
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={() => { draggingRef.current = false; }}
        onContextMenu={handleContextMenu}
      />
      <div className="live-tv-earth-country-picker" aria-label="Choose a country">
        <button
          type="button"
          className={clsx('live-tv-country-button', !selectedCountry && 'active')}
          onClick={() => onCountryChange('')}
        >
          <span>🌍</span>
          <strong>Global</strong>
        </button>
        {sortedCountries.map(country => (
          <button
            key={country.code}
            type="button"
            className={clsx('live-tv-country-button', selectedCountry === country.code && 'active')}
            onClick={() => onCountryChange(country.code)}
            title={`${country.name} · ${country.channelCount.toLocaleString()} channels`}
          >
            <span>{country.code}</span>
            <strong>{country.name}</strong>
            <small>{country.channelCount.toLocaleString()}</small>
          </button>
        ))}
      </div>
    </div>
  );
}
