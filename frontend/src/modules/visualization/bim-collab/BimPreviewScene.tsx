import { Canvas } from '@react-three/fiber';
import { Edges, Html, OrbitControls } from '@react-three/drei';
import type { CSSProperties } from 'react';

import type { BimSceneNode } from './types';

type Vec3 = [number, number, number];
type SceneStatus = BimSceneNode['status'];

interface SceneSpec extends BimSceneNode {
  position: Vec3;
  scale: Vec3;
  color: string;
}

interface BimPreviewSceneProps {
  nodes: BimSceneNode[];
  viewerAvailable: boolean;
  viewerMessage: string;
}

type CssVarStyle = CSSProperties & Record<`--${string}`, string | number>;

const statusColors: Record<SceneStatus, string> = {
  normal: '#49d27d',
  warning: '#f5b400',
  danger: '#f05d52',
  focus: '#18c9df'
};

const placements: Vec3[] = [
  [-3.1, 1.25, -0.85],
  [-1.25, 1.55, 0.38],
  [0.72, 2.15, -0.38],
  [2.52, 1.38, -0.98],
  [3.15, 0.95, 1.35],
  [0.32, 0.72, 2.48]
];

export default function BimPreviewScene({
  nodes,
  viewerAvailable,
  viewerMessage
}: BimPreviewSceneProps) {
  const specs = buildSceneSpecs(nodes);
  const hasModelPreview = specs.length > 0;

  return (
    <div className="sc-bim-scene" aria-label="BIM 模型元数据孪生视图">
      <div className="sc-bim-scene__chrome">
        <strong>{hasModelPreview ? (viewerAvailable ? '模型适配视图' : '模型元数据视图') : '暂无模型预览'}</strong>
        <span>{hasModelPreview ? viewerMessage : '当前项目暂未登记模型文件，请先在模型集成中接入项目模型。'}</span>
      </div>

      {hasModelPreview ? (
        <>
          <div className="sc-bim-scene__fallback" aria-hidden="true">
            {specs.map((spec, index) => (
              <div
                key={spec.id}
                className={`sc-bim-fallback-building is-${spec.status}`}
                style={fallbackBlockStyle(index, spec)}
              >
                <span>{spec.label}</span>
                <small>{spec.meta}</small>
              </div>
            ))}
          </div>

          <Canvas
            camera={{ position: [8.7, 7.1, 10.3], fov: 42 }}
            dpr={[1, 1.65]}
            gl={{ antialias: true, alpha: true }}
            shadows
          >
            <ambientLight intensity={1.12} />
            <hemisphereLight args={['#bdefff', '#07233a', 1.35]} />
            <directionalLight
              castShadow
              position={[4.8, 7.4, 4.6]}
              intensity={2.2}
              shadow-mapSize={[1024, 1024]}
            />
            <pointLight position={[-5.2, 4.4, -2.7]} intensity={1.2} color="#16c7df" />
            <pointLight position={[3.8, 3.8, 3.5]} intensity={0.86} color="#49d27d" />

            <group position={[0.1, -0.24, 0.02]} rotation={[0, -0.36, 0]} scale={0.84}>
              <CampusPlate />
              <CampusGrid />
              <AccessRoads />
              <SceneBuildings specs={specs} />
              <DataPipes />
              <SceneMarkers specs={specs} />
            </group>

            <OrbitControls
              enablePan={false}
              enableZoom={false}
              autoRotate
              autoRotateSpeed={0.26}
              minPolarAngle={Math.PI / 3.25}
              maxPolarAngle={Math.PI / 2.2}
            />
          </Canvas>

          <div className="sc-bim-scene__legend">
            <span><i style={{ background: statusColors.focus }} />定位</span>
            <span><i style={{ background: statusColors.normal }} />已发布</span>
            <span><i style={{ background: statusColors.warning }} />待治理</span>
            <span><i style={{ background: statusColors.danger }} />异常</span>
          </div>
        </>
      ) : (
        <div className="sc-bim-scene__empty">
          <strong>暂无模型预览</strong>
          <span>当前项目没有可轮播的模型，完成模型集成后将在这里按项目模型自动轮播。</span>
        </div>
      )}
    </div>
  );
}

function fallbackBlockStyle(index: number, spec: SceneSpec): CssVarStyle {
  const slots = [
    { x: -34, y: 2, w: 104 },
    { x: -18, y: -5, w: 118 },
    { x: 1, y: -12, w: 112 },
    { x: 20, y: -5, w: 106 },
    { x: 36, y: 6, w: 122 },
    { x: 6, y: 18, w: 210 }
  ];
  const slot = slots[index % slots.length];
  return {
    '--sc-block-x': `${slot.x}%`,
    '--sc-block-y': `${slot.y}%`,
    '--sc-block-w': `${slot.w}px`,
    '--sc-block-h': `${Math.round(58 + spec.scale[1] * 26)}px`,
    '--sc-block-color': statusColors[spec.status]
  };
}

function buildSceneSpecs(nodes: BimSceneNode[]): SceneSpec[] {
  if (nodes.length === 0) return [];
  const maxWeight = Math.max(...nodes.map((item) => item.weight || 1), 1);

  return nodes.slice(0, placements.length).map((node, index) => {
    const weight = Math.max(0.18, Math.min(1, (node.weight || 1) / maxWeight));
    const height = 1.28 + weight * 3.05 + (index % 2) * 0.28;
    const width = 1.18 + (index % 3) * 0.18;
    return {
      ...node,
      position: placements[index],
      scale: [width, height, 1.24 + ((index + 1) % 3) * 0.18],
      color: index % 2 === 0 ? '#1184ad' : '#0f739b'
    };
  });
}

function CampusPlate() {
  return (
    <group>
      <mesh receiveShadow position={[0, -0.08, 0]}>
        <boxGeometry args={[10.6, 0.16, 7.5]} />
        <meshStandardMaterial color="#042b43" roughness={0.82} metalness={0.08} />
        <Edges color="#20c6de" />
      </mesh>
      <mesh position={[0.18, 0.028, 0.16]}>
        <boxGeometry args={[8.86, 0.035, 5.78]} />
        <meshStandardMaterial color="#063956" roughness={0.74} metalness={0.05} />
      </mesh>
    </group>
  );
}

function CampusGrid() {
  return (
    <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
      <planeGeometry args={[10.05, 6.95, 24, 16]} />
      <meshBasicMaterial color="#27d0e6" wireframe transparent opacity={0.13} />
    </mesh>
  );
}

function AccessRoads() {
  return (
    <group>
      <Road position={[0, 0.06, -2.28]} scale={[8.65, 0.055, 0.36]} />
      <Road position={[-2.05, 0.065, 0.45]} scale={[0.34, 0.055, 5.45]} />
      <Road position={[2.54, 0.065, 0.76]} scale={[0.28, 0.055, 4.26]} rotation={[0, 0.42, 0]} />
      <Road position={[0.28, 0.07, 2.18]} scale={[5.7, 0.05, 0.3]} />
      {[-3.8, -2.4, -1, 0.4, 1.8, 3.2].map((x) => (
        <mesh key={x} position={[x, 0.095, -2.28]} scale={[0.5, 0.012, 0.035]}>
          <boxGeometry args={[1, 1, 1]} />
          <meshBasicMaterial color="#74e8f5" transparent opacity={0.72} />
        </mesh>
      ))}
    </group>
  );
}

function Road({ position, scale, rotation = [0, 0, 0] }: { position: Vec3; scale: Vec3; rotation?: Vec3 }) {
  return (
    <mesh position={position} rotation={rotation} scale={scale}>
      <boxGeometry args={[1, 1, 1]} />
      <meshStandardMaterial color="#0b506c" roughness={0.68} metalness={0.1} />
    </mesh>
  );
}

function SceneBuildings({ specs }: { specs: SceneSpec[] }) {
  return (
    <group>
      {specs.map((spec, index) => (
        <Building key={spec.id} spec={spec} index={index} />
      ))}
      {specs.length > 2 ? <Bridge position={[-0.18, 1.02, -0.58]} scale={[2.28, 0.24, 0.34]} /> : null}
      {specs.length > 3 ? <Bridge position={[1.78, 0.9, 0.34]} scale={[2.04, 0.22, 0.32]} rotation={[0, -0.2, 0]} /> : null}
    </group>
  );
}

function Building({ spec, index }: { spec: SceneSpec; index: number }) {
  const statusColor = statusColors[spec.status];
  const accent = spec.status === 'focus' ? '#f5fcff' : '#8df2ff';

  return (
    <group position={spec.position}>
      <mesh castShadow receiveShadow scale={spec.scale}>
        <boxGeometry args={[1, 1, 1]} />
        <meshStandardMaterial color={spec.color} roughness={0.44} metalness={0.22} />
        <Edges color={accent} />
      </mesh>

      <mesh position={[0, spec.scale[1] * 0.51, 0]} scale={[spec.scale[0] * 0.96, 0.055, spec.scale[2] * 0.96]}>
        <boxGeometry args={[1, 1, 1]} />
        <meshStandardMaterial color="#d8f6ff" roughness={0.38} metalness={0.08} />
      </mesh>

      <FacadeGrid height={spec.scale[1]} width={spec.scale[0]} depth={spec.scale[2]} accent={accent} />
      <StatusBeacon color={statusColor} position={[spec.scale[0] * 0.36, spec.scale[1] * 0.63, spec.scale[2] * 0.36]} />

      <Html position={[0, spec.scale[1] * 0.58, -spec.scale[2] * 0.52]} center occlude={false}>
        <div className="sc-bim-roof-tag" style={{ borderColor: statusColor }}>
          {`M-${String(index + 1).padStart(2, '0')}`}
        </div>
      </Html>
    </group>
  );
}

function FacadeGrid({
  height,
  width,
  depth,
  accent
}: {
  height: number;
  width: number;
  depth: number;
  accent: string;
}) {
  const levelCount = Math.max(3, Math.round(height * 3.2));
  const floorLines = Array.from({ length: levelCount }, (_, index) => -height / 2 + (index + 1) * (height / (levelCount + 1)));
  const cols = Array.from({ length: Math.max(2, Math.round(width * 2.4)) }, (_, index) => -width / 2 + ((index + 1) * width) / 4);

  return (
    <group>
      {floorLines.map((y) => (
        <mesh key={`front-floor-${y}`} position={[0, y, depth / 2 + 0.011]} scale={[width * 0.88, 0.012, 0.012]}>
          <boxGeometry args={[1, 1, 1]} />
          <meshBasicMaterial color={accent} transparent opacity={0.26} />
        </mesh>
      ))}
      {cols.map((x) => (
        <mesh key={`front-col-${x}`} position={[x, 0, depth / 2 + 0.012]} scale={[0.012, height * 0.84, 0.012]}>
          <boxGeometry args={[1, 1, 1]} />
          <meshBasicMaterial color={accent} transparent opacity={0.16} />
        </mesh>
      ))}
      {floorLines.map((y) => (
        <mesh key={`side-floor-${y}`} position={[width / 2 + 0.011, y, 0]} scale={[0.012, 0.012, depth * 0.84]}>
          <boxGeometry args={[1, 1, 1]} />
          <meshBasicMaterial color={accent} transparent opacity={0.18} />
        </mesh>
      ))}
    </group>
  );
}

function Bridge({
  position,
  scale,
  rotation = [0, 0, 0]
}: {
  position: Vec3;
  scale: Vec3;
  rotation?: Vec3;
}) {
  return (
    <mesh castShadow position={position} scale={scale} rotation={rotation}>
      <boxGeometry args={[1, 1, 1]} />
      <meshStandardMaterial color="#0b6e90" roughness={0.42} metalness={0.18} transparent opacity={0.88} />
      <Edges color="#a9f4ff" />
    </mesh>
  );
}

function StatusBeacon({ position, color }: { position: Vec3; color: string }) {
  return (
    <group position={position}>
      <mesh>
        <sphereGeometry args={[0.08, 18, 18]} />
        <meshStandardMaterial color={color} emissive={color} emissiveIntensity={0.75} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.13, 0.23, 32]} />
        <meshBasicMaterial color={color} transparent opacity={0.45} />
      </mesh>
    </group>
  );
}

function DataPipes() {
  return (
    <group>
      <Pipe position={[-2.25, 0.16, 2.46]} scale={[3.6, 0.03, 0.03]} color="#31d7e7" />
      <Pipe position={[1.35, 0.16, 2.46]} scale={[3.25, 0.03, 0.03]} color="#31d7e7" />
      <Pipe position={[3.28, 0.16, 0.3]} scale={[0.03, 0.03, 3.78]} color="#f05d52" />
      <Pipe position={[-4.1, 0.16, 0.18]} scale={[0.03, 0.03, 4.1]} color="#f5b400" />
    </group>
  );
}

function Pipe({ position, scale, color }: { position: Vec3; scale: Vec3; color: string }) {
  return (
    <mesh position={position} scale={scale}>
      <boxGeometry args={[1, 1, 1]} />
      <meshBasicMaterial color={color} transparent opacity={0.52} />
    </mesh>
  );
}

function SceneMarkers({ specs }: { specs: SceneSpec[] }) {
  return (
    <group>
      {specs.map((spec) => (
        <Html key={spec.id} position={[spec.position[0], spec.position[1] + spec.scale[1] * 0.68, spec.position[2] - 0.4]} occlude={false}>
          <div className={`sc-bim-model-label is-${spec.status}`}>
            <span>{spec.label}</span>
            <small>{spec.meta}</small>
          </div>
        </Html>
      ))}
    </group>
  );
}
