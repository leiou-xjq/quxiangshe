package com.quxiangshe.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ACAutomaton {
    
    private final Map<Integer, Map<Character, Integer>> gotoFunc = new HashMap<>();
    private final Map<Integer, String> output = new HashMap<>();
    private final Map<Integer, Set<String>> failure = new HashMap<>();
    private int nextState = 0;
    
    public ACAutomaton() {
        gotoFunc.put(0, new HashMap<>());
    }
    
    public void addPattern(String word) {
        int currentState = 0;
        for (char c : word.toCharArray()) {
            Map<Character, Integer> transitions = gotoFunc.get(currentState);
            Integer nextState = transitions.get(c);
            
            if (nextState == null) {
                nextState = ++this.nextState;
                transitions.put(c, nextState);
                gotoFunc.put(nextState, new HashMap<>());
            }
            currentState = nextState;
        }
        
        output.put(currentState, word);
    }
    
    public void build() {
        Queue<Integer> queue = new LinkedList<>();
        
        Map<Character, Integer> level0 = gotoFunc.get(0);
        for (Map.Entry<Character, Integer> entry : level0.entrySet()) {
            int nextState = entry.getValue();
            queue.add(nextState);
            failure.put(nextState, new HashSet<>());
        }
        
        while (!queue.isEmpty()) {
            int currentState = queue.poll();
            
            for (Map.Entry<Character, Integer> entry : 
                    gotoFunc.get(currentState).entrySet()) {
                char c = entry.getKey();
                int nextState = entry.getValue();
                queue.add(nextState);
                
                int failState = failure.containsKey(currentState) ? 
                        getFailureState(currentState, c) : 0;
                
                Map<Character, Integer> failTransitions = gotoFunc.get(failState);
                Integer failTarget = failTransitions != null ? failTransitions.get(c) : null;
                
                if (failTarget != null) {
                    failure.put(nextState, failure.get(failTarget));
                } else if (failState == 0) {
                    failure.put(nextState, new HashSet<>());
                } else {
                    Set<String> failSet = failure.get(failState);
                    failure.put(nextState, failSet != null ? new HashSet<>(failSet) : new HashSet<>());
                }
                
                if (failTarget != null && output.containsKey(failTarget)) {
                    Set<String> failOutput = failure.get(nextState);
                    if (failOutput == null) {
                        failOutput = new HashSet<>();
                        failure.put(nextState, failOutput);
                    }
                    failOutput.add(output.get(failTarget));
                }
            }
        }
    }
    
    private int getFailureState(int state, char c) {
        Set<String> failSet = failure.get(state);
        if (failSet == null || failSet.isEmpty()) {
            return 0;
        }
        
        int failState = state;
        while (failState != 0) {
            Map<Character, Integer> failTransitions = gotoFunc.get(failState);
            Integer target = failTransitions != null ? failTransitions.get(c) : null;
            if (target != null) {
                return target;
            }
            failState = getParentFailureState(failState);
        }
        return 0;
    }
    
    private int getParentFailureState(int state) {
        for (Map.Entry<Integer, Set<String>> entry : failure.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(String.valueOf(state))) {
                return entry.getKey();
            }
        }
        return 0;
    }
    
    public List<String> match(String text) {
        List<String> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        
        int currentState = 0;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            Map<Character, Integer> transitions = gotoFunc.get(currentState);
            Integer nextState = transitions != null ? transitions.get(c) : null;
            
            if (nextState != null) {
                currentState = nextState;
            } else {
                while (currentState != 0 && (transitions == null || transitions.get(c) == null)) {
                    currentState = 0;
                    transitions = gotoFunc.get(currentState);
                    nextState = transitions != null ? transitions.get(c) : null;
                    if (nextState != null) {
                        currentState = nextState;
                        break;
                    }
                }
            }
            
            if (output.containsKey(currentState)) {
                matches.add(output.get(currentState));
            }
            Set<String> failSet = failure.get(currentState);
            if (failSet != null && !failSet.isEmpty()) {
                matches.addAll(failSet);
            }
        }
        
        return matches;
    }
    
    public void clear() {
        gotoFunc.clear();
        output.clear();
        failure.clear();
        nextState = 0;
        gotoFunc.put(0, new HashMap<>());
    }
}