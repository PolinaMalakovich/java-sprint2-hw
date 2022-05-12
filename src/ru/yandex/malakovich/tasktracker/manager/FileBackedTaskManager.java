package ru.yandex.malakovich.tasktracker.manager;

import ru.yandex.malakovich.tasktracker.model.Epic;
import ru.yandex.malakovich.tasktracker.model.Status;
import ru.yandex.malakovich.tasktracker.model.Subtask;
import ru.yandex.malakovich.tasktracker.model.Task;
import ru.yandex.malakovich.tasktracker.model.Type;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static ru.yandex.malakovich.tasktracker.model.Status.DONE;
import static ru.yandex.malakovich.tasktracker.model.Status.IN_PROGRESS;
import static ru.yandex.malakovich.tasktracker.model.Status.NEW;
import static ru.yandex.malakovich.tasktracker.model.Type.EPIC;
import static ru.yandex.malakovich.tasktracker.model.Type.SUBTASK;
import static ru.yandex.malakovich.tasktracker.model.Type.TASK;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();

        return epic;
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();

        return task;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = super.getSubtaskById(id);
        save();

        return subtask;
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        save();
    }

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        save();
    }

    @Override
    public void createSubtask(Subtask subtask) {
        super.createSubtask(subtask);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    private void save() {
        try (Writer fileWriter = new FileWriter(file)) {
            List<Task> list = new ArrayList<>();
            list.addAll(getSubtasks());
            list.addAll(getTasks());
            list.addAll(getEpics());

            fileWriter.write("id,type,name,status,description,epic");
            fileWriter.write(System.lineSeparator());

            for (Task task : list) {
                fileWriter.write(taskToString(task));
                fileWriter.write(System.lineSeparator());
            }

            fileWriter.write(System.lineSeparator());
            fileWriter.write(historyManagerToString(historyManager));

        } catch (IOException exception) {
            throw new ManagerSaveException();
        }
    }

    static FileBackedTaskManager loadFromFile(File file) throws IOException {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);
        Map<Integer, Task> allTasks = new HashMap<>();
        Map<Integer, List<Integer>> subtasks = new HashMap<>();

        List<String> list = Files.readAllLines(file.toPath());
        int splitter = list.lastIndexOf("");

        if (splitter >= 0) {
            for (String item : list.subList(1, splitter)) {
                Task task = taskFromString(item);
                allTasks.put(task.getId(), task);

                if (task instanceof Subtask) {
                    Subtask subtask = (Subtask) task;
                    manager.subtasks.put(task.getId(), subtask);
                    subtasks.putIfAbsent(subtask.getEpicId(), new ArrayList<>());
                    subtasks.get(subtask.getEpicId()).add(subtask.getId());
                } else if (task instanceof Epic) {
                    Epic epic = (Epic) task;
                    if (subtasks.containsKey(epic.getId())) epic.getSubtasks().addAll(subtasks.get(epic.getId()));
                    manager.epics.put(task.getId(), epic);
                } else {
                    manager.tasks.put(task.getId(), task);
                }
            }

            String history = list.get(list.size() - 1);
            List<Integer> ids = historyManagerFromString(history);

            for (Integer id : ids) {
                manager.historyManager.add(allTasks.get(id));
            }

        } else {
            throw new IOException("Некорректный формат файла");
        }

        return manager;
    }

    private static String historyManagerToString(HistoryManager manager) {
        StringBuilder builder = new StringBuilder();

        if (manager != null) {
            List<Task> list = manager.getHistory();

            if (!list.isEmpty()) {
                for (Task task : list) {
                    builder.append(task.getId()).append(",");
                }

                builder.deleteCharAt(builder.length() - 1);
            }
        }

        return builder.toString();
    }

    private static List<Integer> historyManagerFromString(String value) {
        List<Integer> list = new ArrayList<>();

        if (value != null) {
            String[] values = value.split(",");

            for (String element : values) {
                list.add(Integer.parseInt(element));
            }
        }

        return list;
    }

    private static String taskToString(Task task) {
        String element = null;
        Type type = task instanceof Subtask ? SUBTASK : task instanceof Epic ? EPIC : TASK;

        if (task != null) {
            element = task.getId() + ","
                    + type.name() + ","
                    + task.getTitle() + ","
                    + task.getStatus() + ","
                    + task.getDescription();
            if (task instanceof Subtask) {
                Subtask subtask = (Subtask) task;
                element = element + "," + subtask.getEpicId();
            }
        }

        return element;
    }

    static private Task taskFromString(String value) {
        Task task = null;

        if (value != null) {
            String[] values = value.split(",");
            Status status = null;

            switch (values[3]) {
                case "NEW":
                    status = NEW;
                    break;
                case "IN_PROGRESS":
                    status = IN_PROGRESS;
                    break;
                case "DONE":
                    status = DONE;
                    break;
            }

            if (values[1].equals(SUBTASK.name())) {
                task = new Subtask(values[2], values[4], status, Integer.parseInt(values[5]), Integer.parseInt(values[0]));
            } else if (values[1].equals(EPIC.name())) {
                task = Epic.create(values[2], values[4], new HashSet<>(), Integer.parseInt(values[0]));
            } else {
                task = new Task(values[2], values[4], status, Integer.parseInt(values[0]));
            }
        }

        return task;
    }

    public static void main(String[] args) throws IOException {
        FileBackedTaskManager manager = new FileBackedTaskManager(new File("taskManager.csv"));

        Task task1 = new Task("go to the supermarket", "buy groceries for the week", getId());
        manager.createTask(task1);

        Task task2 = new Task("teach the dog a new trick", "fetch", getId());
        manager.createTask(task2);

        Epic epic1 = Epic.create("prepare for the birthday party", "see subtasks", new HashSet<>(), getId());
        manager.createEpic(epic1);

        Epic epic2 = Epic.create("complete The Last of Us II", "get all the trophies", new HashSet<>(), getId());
        manager.createEpic(epic2);

        Subtask subtask1 = new Subtask("order a birthday cake", "call the bakery", NEW, epic1.getId(), getId());
        manager.createSubtask(subtask1);

        Subtask subtask2 = new Subtask("put up decorations", "blow up balloons", NEW, epic1.getId(), getId());
        manager.createSubtask(subtask2);

        manager.getTaskById(task2.getId());
        manager.getEpicById(epic1.getId());
        manager.getSubtaskById(subtask1.getId());

        System.out.println("First manager");
        System.out.println();
        System.out.println("Epics: " + manager.getEpics());
        System.out.println("Tasks: " + manager.getTasks());
        System.out.println("Subtasks: " + manager.getSubtasks());
        System.out.println();
        System.out.println("History: " + manager.history());

        System.out.println("Load from file");
        TaskManager managerFromFile = loadFromFile(Paths.get("taskManager.csv").toFile());

        System.out.println();
        System.out.println("Manager from file");
        System.out.println();
        System.out.println("Epics: " + managerFromFile.getEpics());
        System.out.println("Tasks: " + managerFromFile.getTasks());
        System.out.println("Subtasks: " + managerFromFile.getSubtasks());
        System.out.println();
        System.out.println("History: " + managerFromFile.history());
    }
}