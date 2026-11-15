# Log Book 

**28 July (week 1)**

We formed our team this week and decided to keep the name GAASK for now. Our team members are Sri Sharada Kalyanaraman, Kirill Korzunov, Aaron Pang, Alexandra Lowe and me. We also made a team contract where we agreed to respect each other, contribute equally and complete our weekly work on time. I think having these expectations from the beginning will make it easier for us to work together, especially since the project will involve everyone working on different parts.

Minh 

**4 August (week 2)**

Alexandra created a GitHub repository for our team and we worked on setting up Git on our laptops. We had some trouble with generating the SSH key and connecting it to our Git account, which took up most of the class, so we did not get to complete the epic activity. We eventually got the repository working and everyone was able to access and make changes to it. This made me realise how important it is to get Git set up early because we will rely on it to share and track our code throughout the project.

Minh

**11 August (week 3)**

There were some proposals to be discussed for our simulation, including the cockpit view, a plant life cycle, and even the ant army before we finally opted for the hill ecosystem simulation. The idea is to include some plants, such as flowers and weeds, on the ground level, while the sky is responsible for creating the various weather conditions on those plants. Furthermore, we did a UML diagram draft. In my opinion, it was quite helpful for us as it helped to brainstorm some design issues.

Minh

**18 August (week 4)**

This week, we reviewed the code that we had worked on till now and established a base for the project. In addition, we began to think of how we might implement inheritance among the different varieties of plants. Rather than writing the same code for all the plants, we will have a general Plant class and then create specialised plants using the Plant class. Furthermore, we also agreed that we must make comments wherever necessary while committing and use Discord in case we encounter any issues. This gave me insight into how the concepts of inheritance and polymorphism learned in class could actually be useful to us.

Minh

**25 August (week 5)**

We have been taught about the use of generics and how they help us to ensure type safety when used at compile-time. My interest was more inclined towards bounded generics since we make use of Patch<T extends Growable>. This signifies that the patch will operate on objects that are of type Growable. It helped me understand generics in a better way as I got to see their practical use.

Minh

**1 September (week 6)**

We learned about exception handling, where we learned about concepts such as try-catch, and how exceptions could be propagated through methods. In our project, we have used these concepts in our implementation with InvalidWeatherException, which helps us in dealing with invalid values for weather and time. I found that it was interesting to learn about throwing an exception from one point in a code and then handling it somewhere else rather than handling everything in one place.

Minh

**8 September (week 7)**

Different design patterns and principles, including Strategy, Observer and the Open/Closed Principle were discussed today. I found it interesting to think about these ideas in terms of our own project and how we could make the code easier to change later. Since we may want to add more plants or behaviours to the simulation having a structure where new features can be added without changing existing code too much would be useful.

Minh 

**15 September(week 8)**

This week, we continued to explore design patterns such as the Decorator, the Iterator and the State. Also, we had an exercise where we had a debate on the usage of design patterns in various projects. One design pattern that caught my attention is the State design pattern because it enables an object to behave in certain ways according to its current state. It also helped me realize that it does not suffice to use a certain design pattern merely because it exists, but rather for what problem it solves.

**6 October (week 9)**

We discussed topics like behavior parameterization and lambdas. We have seen the usage of functional interfaces like Predicate, Function, Consumer and Supplier and seen that the method references can be used to write more compact code. We solved some programming challenges and the objective was to solve the challenge using lambdas in minimum characters. The part which I found quite fascinating was that using lambdas, we can pass the behaviour as an argument without creating individual classes for each and every behaviour. I started thinking of our simulation and saw that there might be some ways to use lambdas for handling some of the different behaviors of plants/entities.

**13 October (week 10)**

For this week, we looked into the Streams API in Java and its usage in performing operations on collections without the need to use long loops. Stream pipelines and their methods such as filter, map, reduce, and collect were explained. While working on our simulation, we often require ourselves to iterate through collections of plants and other objects and perform some filtering or calculations. I have realized that the use of streams could simplify such processes. 

**20 October(week 11)**

In this week’s lesson, we explored more on Streams and the use of Collectors to sort and analyze data in them. We were introduced to some methods of Collectors like toList(), toSet(), toMap(), and groupingBy(). GroupingBy is a method of Collectors which I liked a lot since it helped me learn how to categorize objects according to a particular attribute and then apply mathematical calculations on each group. I can imagine how groupingBy can be used in our hill ecosystem project, for example: to group different kinds of plants according to their category and growth stage and then count their number.


**27 october(week 12)**

We talked about threads, synchronization, parallel streams and race conditions. Race conditions caught my attention especially as they occur when multiple threads work with the same data without coordinating with each other causing unexpected outcomes. Coming back to our simulation, I understood that there might be some sections of the ecosystem that could be updated simultaneously. However, it would also create problems when multiple threads would try to update the same objects or areas. It became clear that parallelism is not simply about making the program faster.
