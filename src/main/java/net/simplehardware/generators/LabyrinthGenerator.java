package net.simplehardware.generators;
import net.simplehardware.models.CellButton;
import net.simplehardware.MazeGrid;
import net.simplehardware.models.Mode;

import java.util.*;

public class LabyrinthGenerator {

    private static final Random RNG = new Random();

    public static void generateMaze(MazeGrid grid) {
        int n = grid.getGridSize();
        if (n < 5) return;

        CellButton[][] cells = grid.getCells();


        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++)
                cells[x][y].setMode(Mode.WALL, 0);

        boolean[][] visited = new boolean[n][n];
        generateMaze(cells, visited, 1, 1, n);

        for (int i = 0; i < (n * n) / 20; i++) {
            int x = RNG.nextInt(n - 2) + 1; // 1..n-2
            int y = RNG.nextInt(n - 2) + 1;
            if (cells[x][y].getMode() == Mode.WALL)
                cells[x][y].setMode(Mode.FLOOR, 0);
        }

        ensureFullConnectivity(cells, n);

        for (int i = 0; i < n; i++) {
            cells[0][i].setMode(Mode.WALL, 0);
            cells[n - 1][i].setMode(Mode.WALL, 0);
            cells[i][0].setMode(Mode.WALL, 0);
            cells[i][n - 1].setMode(Mode.WALL, 0);
        }

    }

    private static void generateMaze(CellButton[][] cells, boolean[][] visited, int startX, int startY, int n) {
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startX, startY});
        visited[startX][startY] = true;
        cells[startX][startY].setMode(Mode.FLOOR, 0);

        int[][] dirs = {{0, 2}, {2, 0}, {0, -2}, {-2, 0}};

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int x = current[0];
            int y = current[1];

            List<int[]> neighbors = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < n && ny < n && !visited[nx][ny]) {
                    neighbors.add(d);
                }
            }

            if (neighbors.isEmpty()) {
                stack.pop();
                continue;
            }

            int[] d = neighbors.get(RNG.nextInt(neighbors.size()));
            int nx = x + d[0];
            int ny = y + d[1];

            // carve the wall between
            cells[x + d[0] / 2][y + d[1] / 2].setMode(Mode.FLOOR, 0);
            cells[nx][ny].setMode(Mode.FLOOR, 0);
            visited[nx][ny] = true;

            stack.push(new int[]{nx, ny});
        }
    }

    private static class ComponentResult {
        final int[][] ids;
        final int count;
        ComponentResult(int[][] ids, int count) { this.ids = ids; this.count = count; }
    }

    private static void ensureFullConnectivity(CellButton[][] cells, int n) {
        // Repeatedly label components and connect them until there's only one component of floors
        while (true) {
            ComponentResult cr = labelComponents(cells, n);
            if (cr.count <= 1) return; // already fully connected

            int[][] compId = cr.ids;
            int mainComp = compId[1][1]; // DFS started at (1,1), so main component should include it
            if (mainComp == 0) {
                // Defensive: if (1,1) isn't floor for some reason, pick any floor's component as main
                outer:
                for (int x = 1; x < n - 1; x++) {
                    for (int y = 1; y < n - 1; y++) {
                        if (compId[x][y] != 0) {
                            mainComp = compId[x][y];
                            break outer;
                        }
                    }
                }
                if (mainComp == 0) return; // no floor at all (unexpected), nothing to do
            }

            // Find a component that's not the main one and connect it
            boolean connectedOne = false;
            for (int comp = 1; comp <= cr.count; comp++) {
                if (comp == mainComp) continue;

                // find a representative cell in this component
                int repX = -1, repY = -1;
                outer2:
                for (int x = 1; x < n - 1; x++) {
                    for (int y = 1; y < n - 1; y++) {
                        if (compId[x][y] == comp) { repX = x; repY = y; break outer2; }
                    }
                }

                if (repX == -1) continue; // shouldn't happen

                // BFS from rep to nearest cell that belongs to mainComp (we may traverse walls)
                boolean found = connectComponentToMain(cells, compId, repX, repY, mainComp, n);
                if (found) {
                    connectedOne = true;
                    break; // relabel components in next iteration
                } else {
                    // If no path found (very unlikely), try next component
                }
            }

            if (!connectedOne) {
                int mainX=-1, mainY=-1, otherX=-1, otherY=-1;
                for (int x=1;x<n-1 && mainX==-1;x++) for (int y=1;y<n-1;y++) if (compId[x][y]==mainComp) { mainX=x; mainY=y; break; }
                outer3:
                for (int x=1;x<n-1;x++) for (int y=1;y<n-1;y++) if (compId[x][y]!=0 && compId[x][y]!=mainComp) { otherX=x; otherY=y; break outer3; }
                if (mainX!=-1 && otherX!=-1) {
                    // carve L-shaped path
                    int cx = otherX, cy = otherY;
                    while (cx != mainX) {
                        cells[cx][cy].setMode(Mode.FLOOR, 0);
                        cx += (mainX > cx) ? 1 : -1;
                    }
                    while (cy != mainY) {
                        cells[cx][cy].setMode(Mode.FLOOR, 0);
                        cy += (mainY > cy) ? 1 : -1;
                    }
                    // loop again to relabel
                } else {
                    // completely unexpected: break to avoid infinite loop
                    return;
                }
            }
            // loop repeats and relabels components until only one remains
        }
    }

    private static ComponentResult labelComponents(CellButton[][] cells, int n) {
        int[][] ids = new int[n][n];
        int comp = 0;
        int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};

        for (int x = 1; x < n - 1; x++) {
            for (int y = 1; y < n - 1; y++) {
                if (cells[x][y].getMode() == Mode.FLOOR && ids[x][y] == 0) {
                    comp++;
                    Deque<int[]> q = new ArrayDeque<>();
                    q.add(new int[]{x,y});
                    ids[x][y] = comp;
                    while (!q.isEmpty()) {
                        int[] p = q.poll();
                        int px = p[0], py = p[1];
                        for (int[] d : dirs) {
                            int nx = px + d[0], ny = py + d[1];
                            if (nx <= 0 || nx >= n-1 || ny <= 0 || ny >= n-1) continue;
                            if (ids[nx][ny] == 0 && cells[nx][ny].getMode() == Mode.FLOOR) {
                                ids[nx][ny] = comp;
                                q.add(new int[]{nx, ny});
                            }
                        }
                    }
                }
            }
        }
        return new ComponentResult(ids, comp);
    }

    /**
     * BFS from (repX,repY) across the entire interior (walls permitted) until we hit a cell
     * belonging to mainComp (and that cell must be a FLOOR). Then carve the path found.
     * Returns true if a connecting path was carved.
     */
    private static boolean connectComponentToMain(CellButton[][] cells, int[][] compId, int repX, int repY, int mainComp, int n) {
        int size = n * n;
        boolean[] visited = new boolean[size];
        int[] parent = new int[size];
        Arrays.fill(parent, -1);

        int startIdx = repX * n + repY;
        Deque<Integer> q = new ArrayDeque<>();
        q.add(startIdx);
        visited[startIdx] = true;

        int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};
        int targetIdx = -1;

        while (!q.isEmpty()) {
            int idx = q.poll();
            int x = idx / n;
            int y = idx % n;

            // If this cell is part of mainComp and is a floor, we've connected
            if (compId[x][y] == mainComp && cells[x][y].getMode() == Mode.FLOOR) {
                targetIdx = idx;
                break;
            }

            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx <= 0 || nx >= n-1 || ny <= 0 || ny >= n-1) continue; // avoid outer border
                int nidx = nx * n + ny;
                if (!visited[nidx]) {
                    visited[nidx] = true;
                    parent[nidx] = idx;
                    q.add(nidx);
                }
            }
        }

        if (targetIdx == -1) return false; // no path found (should be rare/impossible)

        // reconstruct path from target back to start and carve floors along it
        int cur = targetIdx;
        while (cur != -1) {
            int cx = cur / n;
            int cy = cur % n;
            if (cx > 0 && cx < n-1 && cy > 0 && cy < n-1) {
                cells[cx][cy].setMode(Mode.FLOOR, 0);
            }
            cur = parent[cur];
        }

        return true;
    }
}
