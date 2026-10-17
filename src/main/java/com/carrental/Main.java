package com.carrental;

import com.carrental.cli.Application;
import com.carrental.cli.ConsoleMenu;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Path dataDir = Path.of("data");
        Scanner scanner = new Scanner(System.in);

        boolean reset;
        do {
            Application app = Application.open(dataDir);
            reset = new ConsoleMenu(app.facade(), app.admin(), scanner).run();
            if (reset) {
                Application.reset(dataDir);
                System.out.println("All data has been reset.");
            }
        } while (reset);

        System.out.println("Bye.");
    }
}