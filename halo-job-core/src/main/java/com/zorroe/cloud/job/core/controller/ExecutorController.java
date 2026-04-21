package com.zorroe.cloud.job.core.controller;

import com.zorroe.cloud.job.core.component.JobMethodRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutorController {

    @GetMapping("/executor/run")
    public String run(
            @RequestParam String handler,
            @RequestParam(required = false) String param
    ) {
        JobMethodRegistry.JobMethodHolder holder = JobMethodRegistry.get(handler);
        if (holder == null) {
            return "handler not found: " + handler;
        }

        try {
            Class<?>[] types = holder.getMethod().getParameterTypes();
            if (types.length == 0) {
                holder.getMethod().invoke(holder.getBean());
            } else if (types.length == 1 && types[0] == String.class) {
                holder.getMethod().invoke(holder.getBean(), param);
            }
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}