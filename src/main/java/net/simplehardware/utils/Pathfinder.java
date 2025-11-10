package net.simplehardware.utils;

import net.simplehardware.dialogs.PathfindingErrorDialog;
import net.simplehardware.models.CellButton;
import net.simplehardware.models.Mode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Utility class for finding the shortest path in the maze using BFS algorithm
 */
public class Pathfinder {

    /**
     * Point class to represent coordinates in the maze
     */
    public static class Point {
        public final int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Point)) return false;
            Point other = (Point) obj;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * Finds the shortest path distance between two points in the maze
     * @param grid The maze grid
     * @param start Starting coordinates
     * @param end Ending coordinates
     * @return Minimum number of moves, or -1 if no path exists
     */
    public static int findShortestPath(CellButton[][] grid, Point start, Point end) {
        if (start.equals(end)) {
            return 0;
        }

        int n = grid.length;
        boolean[][] visited = new boolean[n][n];
        Queue<Point> queue = new LinkedList<>();
        Queue<Integer> distanceQueue = new LinkedList<>();

        queue.add(start);
        visited[start.x][start.y] = true;
        distanceQueue.add(0);

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int currentDist = distanceQueue.poll();

            for (int i = 0; i < 4; i++) {
                int newX = current.x + dx[i];
                int newY = current.y + dy[i];

                if (newX < 0 || newX >= n || newY < 0 || newY >= n) {
                    continue;
                }

                if (visited[newX][newY]) {
                    continue;
                }

                if (grid[newX][newY].getMode() == Mode.WALL) {
                    continue;
                }

                Point neighbor = new Point(newX, newY);

                if (neighbor.equals(end)) {
                    return currentDist + 1;
                }

                visited[newX][newY] = true;
                queue.add(neighbor);
                distanceQueue.add(currentDist + 1);
            }
        }

        return -1;
    }

    /**
     * Finds all positions of a specific mode in the maze
     * @param grid The maze grid
     * @param targetMode The mode to search for
     * @param targetPlayerId The player ID to match (0 for any player)
     * @return List of points where the target mode is found
     */
    public static List<Point> findPositions(CellButton[][] grid, Mode targetMode, int targetPlayerId) {
        List<Point> positions = new ArrayList<>();
        int n = grid.length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                CellButton cell = grid[i][j];
                if (cell.getMode() == targetMode &&
                        (targetPlayerId == 0 || cell.getPlayerId() == targetPlayerId)) {
                    positions.add(new Point(i, j));
                }
            }
        }

        return positions;
    }

    /**
     * Calculates the total minimum moves to collect all forms in alphabetical order
     * and reach the finish, starting from the start position
     * @param grid The maze grid
     * @param startPlayerId The player ID to use for start, forms, and finish positions
     * @return Total minimum moves, or -1 if path doesn't exist
     */
    public static int calculateMinimumMoves(CellButton[][] grid, int startPlayerId) {
        List<Point> startPositions = findPositions(grid, Mode.START, startPlayerId);
        if (startPositions.isEmpty()) {
            new PathfindingErrorDialog(null,
                "No start position found for player " + startPlayerId).show();
            return -1;
        }
        Point startPoint = startPositions.get(0);

        List<Point> finishPositions = findPositions(grid, Mode.FINISH, startPlayerId);
        if (finishPositions.isEmpty()) {
            new PathfindingErrorDialog(null,
                "No finish position found for player " + startPlayerId).show();
            return -1;
        }
        Point finishPoint = finishPositions.get(0);

        List<Mode> formModes = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            Mode formMode = Mode.valueOf("FORM_" + c);
            formModes.add(formMode);
        }
        
        List<Point> formPoints = new ArrayList<>();
        for (Mode formMode : formModes) {
            List<Point> positions = findPositions(grid, formMode, startPlayerId);
            if (!positions.isEmpty()) {
                formPoints.add(positions.get(0));
            }
        }
        
        if (formPoints.isEmpty()) {
            int directPath = findShortestPath(grid, startPoint, finishPoint);
            if (directPath == -1) {
                new PathfindingErrorDialog(null,
                    "No path exists from start to finish").show();
                return -1;
            }
            return directPath;
        }

        int totalMoves = 0;
        Point currentPoint = startPoint;

        int pathToFirstForm = findShortestPath(grid, currentPoint, formPoints.get(0));
        if (pathToFirstForm == -1) {
            new PathfindingErrorDialog(null,
                "No path exists from start to first form").show();
            return -1;
        }
        totalMoves += pathToFirstForm;
        currentPoint = formPoints.get(0);

        for (int i = 1; i < formPoints.size(); i++) {
            int path = findShortestPath(grid, currentPoint, formPoints.get(i));
            if (path == -1) {
                new PathfindingErrorDialog(null,
                    "No path exists between forms " + (char)('A' + i - 1) + " and " + (char)('A' + i)).show();
                return -1;
            }
            totalMoves += path;
            currentPoint = formPoints.get(i);
        }

        int pathToFinish = findShortestPath(grid, currentPoint, finishPoint);
        if (pathToFinish == -1) {
            new PathfindingErrorDialog(null,
                "No path exists from last form to finish").show();
            return -1;
        }
        totalMoves += pathToFinish;
        
        return totalMoves;
    }
}