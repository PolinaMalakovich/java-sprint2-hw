package ru.yandex.malakovich.tasktracker.manager;

import ru.yandex.malakovich.tasktracker.model.Epic;
import ru.yandex.malakovich.tasktracker.model.Subtask;
import ru.yandex.malakovich.tasktracker.model.Task;
import ru.yandex.malakovich.tasktracker.util.ManagerUtils;
import ru.yandex.malakovich.tasktracker.util.Managers;
import ru.yandex.malakovich.tasktracker.util.TaskUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class InMemoryTaskManager implements TaskManager {
    private int id = 1;
    // Наставник Сергей Савельев сказал, что их нужно оставить protected,
    // смотри ответы в этом треде https://yandex-students.slack.com/archives/C03392E7N69/p1652367547922429
    // также наставник сказал, что .idea и проектный .iml не нужны (см. тот же тред)
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    protected final Set<Task> prioritizedTasks = new TreeSet<>(TaskUtils.START_TIME_TASK_COMPARATOR);

    @Override
    public List<Epic> getEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public List<Task> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public List<Task> getPrioritizedTasksList() {
        return new ArrayList<>(prioritizedTasks);
    }

    public int getNextId() {
        return id++;
    }

    protected void setStartingId(int id) {
        this.id = id;
    }

    @Override
    public void deleteAllEpics() {
        for (Epic epic : epics.values()) {
            historyManager.remove(epic);
        }
        epics.clear();
        deleteAllSubtasks();
    }

    @Override
    public void deleteAllTasks() {
        for (Task task : tasks.values()) {
            historyManager.remove(task);
            prioritizedTasks.remove(task);
        }
        tasks.clear();
    }

    @Override
    public void deleteAllSubtasks() {
        for (Subtask subtask : subtasks.values()) {
            historyManager.remove(subtask);
            prioritizedTasks.remove(subtask);
        }
        subtasks.clear();
    }

    @Override
    public Epic getEpicById(int id) {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            historyManager.add(epic);

            return epic;
        } else {
            System.out.println("Epic not found, id=" + id);

            return null;
        }
    }

    @Override
    public Task getTaskById(int id) {
        if (tasks.containsKey(id)) {
            Task task = tasks.get(id);
            historyManager.add(task);

            return task;
        } else {
            System.out.println("Task not found, id=" + id);

            return null;
        }
    }

    @Override
    public Subtask getSubtaskById(int id) {
        if (subtasks.containsKey(id)) {
            Subtask subtask = subtasks.get(id);
            historyManager.add(subtask);

            return subtask;
        } else {
            System.out.println("Subtask not found, id=" + id);

            return null;
        }

    }

    @Override
    public Epic createEpic(Epic epic) {
        Epic newEpic = null;
        if (epic != null) {
            newEpic = epic.getId() == null ? epic.withId(getNextId()) : epic;
            epics.put(newEpic.getId(), newEpic);
        }

        return newEpic;
    }

    @Override
    public Task createTask(Task task) {
        Task newTask = null;
        if (task != null) {
            ManagerUtils.validateTime(task, prioritizedTasks);
            newTask = task.getId() == null ? task.withId(getNextId()) : task;
            prioritizedTasks.add(newTask);
            tasks.put(newTask.getId(), newTask);
        }

        return newTask;
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        Subtask newSubtask = null;
        if (subtask != null) {
            Epic oldEpic = getEpicById(subtask.getEpicId());
            newSubtask = createSubtaskWorker(oldEpic, subtask);
        }

        return newSubtask;
    }

    protected Subtask createSubtaskWorker(Epic oldEpic, Subtask subtask) {
        Subtask newSubtask = null;
        if (oldEpic != null) {
            ManagerUtils.validateTime(subtask, prioritizedTasks);
            newSubtask = subtask.getId() == null ? subtask.withId(getNextId()) : subtask;
            prioritizedTasks.add(newSubtask);
            subtasks.put(newSubtask.getId(), newSubtask);
            Set<Subtask> newSubtasks = new HashSet<>(getEpicSubtasks(oldEpic));
            newSubtasks.add(newSubtask);
            Epic epic = Epic.create(oldEpic.getId(), oldEpic.getDescription(), newSubtasks, oldEpic.getTitle());
            epics.replace(epic.getId(), epic);
        }

        return newSubtask;
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epic != null) {
            epics.replace(epic.getId(), epic);
        }
    }

    @Override
    public void updateTask(Task task) {
        if (task != null) {
            ManagerUtils.validateTime(task, prioritizedTasks);
            Task oldTask = tasks.get(task.getId());
            if (oldTask != null) {
                prioritizedTasks.remove(oldTask);
                prioritizedTasks.add(task);
            }
            tasks.replace(task.getId(), task);
        }
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtask != null) {
            ManagerUtils.validateTime(subtask, prioritizedTasks);
            Subtask oldSubtask = getSubtaskById(subtask.getId());
            if (oldSubtask != null) {
                deleteSubtaskById(oldSubtask.getId());
                createSubtask(subtask);
            }
        }
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = getEpicById(id);
        if (epic != null) {
            for (int i : epic.getSubtasks()) {
                deleteSubtaskById(i);
            }
            historyManager.remove(epic);
            epics.remove(id);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        if (tasks.containsKey(id)) {
            Task task = tasks.get(id);
            prioritizedTasks.remove(task);
            historyManager.remove(task);
            tasks.remove(id);
        } else {
            System.out.println("Could not delete task, id=" + id);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = getSubtaskById(id);
        if (subtask != null) {
            Epic oldEpic = getEpicById(subtask.getEpicId());
            if (oldEpic != null) {
                Set<Subtask> newSubtasks = new HashSet<>(getEpicSubtasks(oldEpic));
                newSubtasks.remove(subtask);
                Epic epic = Epic.create(oldEpic.getId(), oldEpic.getDescription(), newSubtasks, oldEpic.getTitle());
                updateEpic(epic);
                prioritizedTasks.remove(subtask);
                historyManager.remove(subtask);
                subtasks.remove(id);
            }
        }
    }

    @Override
    public Set<Subtask> getEpicSubtasks(Epic epic) {
        Set<Subtask> epicSubtasks = new HashSet<>();

        if (epic != null) {
            for (int id : epic.getSubtasks()) {
                epicSubtasks.add(subtasks.get(id));
            }
        }

        return epicSubtasks;
    }

    @Override
    public List<Task> history() {
        return historyManager.getHistory();
    }
}
