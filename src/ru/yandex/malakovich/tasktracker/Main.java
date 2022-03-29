package ru.yandex.malakovich.tasktracker;

import ru.yandex.malakovich.tasktracker.manager.InMemoryTaskManager;
import ru.yandex.malakovich.tasktracker.manager.TaskManager;
import ru.yandex.malakovich.tasktracker.model.Epic;
import ru.yandex.malakovich.tasktracker.model.Status;
import ru.yandex.malakovich.tasktracker.model.Subtask;
import ru.yandex.malakovich.tasktracker.model.Task;

import java.util.HashMap;
import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        final IdGenerator idGenerator = new IdGenerator();
        TaskManager manager = new InMemoryTaskManager(new HashMap<>(), new HashMap<>(), new HashMap<>());
        Task task1 = new Task("tidy up", "just do it!", idGenerator.getNewId());
        manager.createTask(task1);
        Task task2 = new Task("walk the dog", "woof-woof", idGenerator.getNewId());
        manager.createTask(task2);
        Epic epic1 = Epic.create("prepare for OCA",
                "life is hard",
                new HashSet<>(),
                idGenerator.getNewId());
        manager.createEpic(epic1);
        Subtask subtask1 = new Subtask("read documentation",
                "it's gonna be so much fun",
                Status.NEW,
                epic1.getId(),
                idGenerator.getNewId());
        manager.createSubtask(subtask1);
        Subtask subtask2 = new Subtask("do practice tests",
                "work work work",
                Status.NEW,
                epic1.getId(),
                idGenerator.getNewId());
        manager.createSubtask(subtask2);
        Epic epic2 = Epic.create("get ready for IELTS",
                "let me speak from my heart",
                new HashSet<>(),
                idGenerator.getNewId());
        manager.createEpic(epic2);

        System.out.println(manager.getEpics());
        System.out.println(manager.getTasks());
        System.out.println(manager.getSubtasks());
        System.out.println();

        Task updatedTask = new Task(task1.getTitle(), task1.getDescription(), Status.IN_PROGRESS, task1.getId());
        manager.updateTask(updatedTask);
        Subtask updatedSubtask = new Subtask(subtask1.getTitle(),
                subtask1.getDescription(),
                Status.IN_PROGRESS,
                subtask1.getEpicId(),
                subtask1.getId());
        manager.updateSubtask(updatedSubtask);

        System.out.println(manager.getEpics());
        System.out.println(manager.getTasks());
        System.out.println(manager.getSubtasks());
        System.out.println();

        manager.deleteEpicById(epic2.getId());
        manager.deleteTaskById(task2.getId());
        manager.deleteSubtaskById(subtask1.getId());

        System.out.println(manager.getEpics());
        System.out.println(manager.getTasks());
        System.out.println(manager.getSubtasks());
        System.out.println();
    }
}
