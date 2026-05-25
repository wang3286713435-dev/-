import type { CatalogDirectory } from '@/modules/data-steward/api/dataSteward';

export interface DirectoryTreeNode {
  id: string;
  name: string;
  fullPath: string;
  relativePath: string;
  fileCount: number;
  totalSizeBytes: number;
  hasChildren: boolean;
  children: DirectoryTreeNode[];
}

export interface DirectoryTreeModel {
  nodes: DirectoryTreeNode[];
  rootFileCount: number;
  labelByPath: Map<string, string>;
}

interface ParsedDirectory {
  directory: CatalogDirectory;
  normalizedPath: string;
  segments: string[];
  hasLeadingSlash: boolean;
}

interface MutableDirectoryTreeNode extends DirectoryTreeNode {
  childMap: Map<string, MutableDirectoryTreeNode>;
}

export function buildDirectoryTree(directories: CatalogDirectory[]): DirectoryTreeModel {
  if (!directories.length) {
    return {
      nodes: [],
      rootFileCount: 0,
      labelByPath: new Map()
    };
  }

  const parsed = directories
    .map(parseDirectory)
    .filter((item) => item.segments.length > 0);

  if (!parsed.length) {
    return {
      nodes: [],
      rootFileCount: 0,
      labelByPath: new Map()
    };
  }

  const prefixLength = 0;
  const labelByPath = new Map<string, string>();
  const rootMap = new Map<string, MutableDirectoryTreeNode>();
  let rootFileCount = 0;

  for (const item of parsed) {
    const relativeSegments = item.segments.slice(prefixLength);
    rootFileCount += item.directory.fileCount;

    if (relativeSegments.length === 0) {
      continue;
    }

    const safeSegments = relativeSegments;

    let currentMap = rootMap;
    let parentNode: MutableDirectoryTreeNode | null = null;

    for (let index = 0; index < safeSegments.length; index += 1) {
      const segment = safeSegments[index];
      const originalSegmentCount = Math.min(item.segments.length, prefixLength + index + 1);
      const fullPath = joinSegments(item.segments.slice(0, originalSegmentCount), item.hasLeadingSlash);
      const relativePath = safeSegments.slice(0, index + 1).join('/');

      let nextNode = currentMap.get(segment);
      if (!nextNode) {
        nextNode = createNode(segment, fullPath, relativePath);
        currentMap.set(segment, nextNode);
        if (parentNode) {
          parentNode.children.push(nextNode);
          parentNode.hasChildren = true;
        }
      }

      nextNode.fileCount += item.directory.fileCount;
      nextNode.totalSizeBytes += item.directory.totalSizeBytes;
      if (index < safeSegments.length - 1 || item.directory.hasChildren) {
        nextNode.hasChildren = true;
      }
      labelByPath.set(fullPath, relativePath);

      parentNode = nextNode;
      currentMap = nextNode.childMap;
    }

  }

  const nodes = Array.from(rootMap.values());
  sortNodes(nodes);

  return {
    nodes: nodes.map(stripChildMap),
    rootFileCount,
    labelByPath
  };
}

function parseDirectory(directory: CatalogDirectory): ParsedDirectory {
  const normalizedPath = normalizeDirectoryPath(directory.directoryPath);
  return {
    directory,
    normalizedPath,
    segments: splitSegments(normalizedPath),
    hasLeadingSlash: normalizedPath.startsWith('/')
  };
}

function normalizeDirectoryPath(path: string): string {
  const trimmed = path.trim();
  if (!trimmed) return '';
  return trimmed.replace(/\/+$/, '');
}

function splitSegments(path: string): string[] {
  return path.split('/').filter(Boolean);
}

function joinSegments(segments: string[], hasLeadingSlash: boolean): string {
  if (!segments.length) return '';
  const joined = segments.join('/');
  return hasLeadingSlash ? `/${joined}` : joined;
}

function createNode(name: string, fullPath: string, relativePath: string): MutableDirectoryTreeNode {
  return {
    id: fullPath || relativePath || name,
    name,
    fullPath,
    relativePath,
    fileCount: 0,
    totalSizeBytes: 0,
    hasChildren: false,
    children: [],
    childMap: new Map()
  };
}

function sortNodes(nodes: MutableDirectoryTreeNode[]) {
  nodes.sort((left, right) => left.name.localeCompare(right.name, 'zh-CN'));
  for (const node of nodes) {
    if (node.children.length) {
      sortNodes(node.children as MutableDirectoryTreeNode[]);
    }
  }
}

function stripChildMap(node: MutableDirectoryTreeNode): DirectoryTreeNode {
  return {
    id: node.id,
    name: node.name,
    fullPath: node.fullPath,
    relativePath: node.relativePath,
    fileCount: node.fileCount,
    totalSizeBytes: node.totalSizeBytes,
    hasChildren: node.hasChildren || node.children.length > 0,
    children: node.children.map((child) => stripChildMap(child as MutableDirectoryTreeNode))
  };
}
