package ru.yandex.malakovich.tasktracker.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.yandex.malakovich.tasktracker.KVTaskClient;
import ru.yandex.malakovich.tasktracker.model.Epic;
import ru.yandex.malakovich.tasktracker.model.Subtask;
import ru.yandex.malakovich.tasktracker.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpTaskManager extends FileBackedTaskManager {
    private final KVTaskClient kvClient;
    private final Gson gson = new Gson();

    public HttpTaskManager(String host) {
        this(host, false);
    }

    public HttpTaskManager(String host, boolean needLoad) {
        super(null);
        kvClient = new KVTaskClient(host);
        if (needLoad) load();
    }

    public void load() {
        String tasksList = kvClient.load("tasks");
        String subtasksList = kvClient.load("subtasks");
        String epicsList = kvClient.load("epics");
        String historyList = kvClient.load("history");

        List<Task> tasks = gson.fromJson(tasksList, new TypeToken<ArrayList<Task>>(){}.getType());
        List<Subtask> subtasks = gson.fromJson(subtasksList, new TypeToken<ArrayList<Subtask>>(){}.getType());
        List<Epic> epics = gson.fromJson(epicsList, new TypeToken<ArrayList<Epic>>(){}.getType());

        Map<Integer, Task> all = Stream
                .concat(tasks.stream(), Stream.concat(subtasks.stream(), epics.stream()))
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        List<Integer> history = gson.fromJson(historyList, new TypeToken<ArrayList<Integer>>(){}.getType());
        history.stream().map(all::get).forEach(this.historyManager::add);

        all.keySet().stream().max(Integer::compare).ifPresent(this::setStartingId);

        tasks.forEach(task -> this.tasks.put(task.getId(), task));
        subtasks.forEach(subtask -> this.subtasks.put(subtask.getId(), subtask));
        epics.forEach(epic -> this.epics.put(epic.getId(), epic));
        this.prioritizedTasks.addAll(tasks);
        this.prioritizedTasks.addAll(subtasks);
    }

    @Override
    public void save() {
        List<Task> tasks = getTasks();
        List<Subtask> subtasks = getSubtasks();
        List<Epic> epics = getEpics();
        List<Integer> history = history().stream().map(Task::getId).collect(Collectors.toList());

        String tasksList = gson.toJson(tasks);
        String subtasksList = gson.toJson(subtasks);
        String epicsList = gson.toJson(epics);
        String historyList = gson.toJson(history);

        kvClient.put("tasks", tasksList);
        kvClient.put("subtasks", subtasksList);
        kvClient.put("epics", epicsList);
        kvClient.put("history", historyList);
    }
}
