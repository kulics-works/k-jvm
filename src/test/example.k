mod demo;

let main() Void = {}

let a Float = 0.1 + 0.3;

let mut b Int = 321 - 300;

let c Int = 2 * 2;

let c2 = 2 * 2;

let mut d Int = b + c * 2;

let myTrue Bool = true;

let myFalse Bool = false;

let chr Rune = 'c';

let str String = "Hello, world!";

let g() Int = 1 + 2 + 3 + b;

let g2() = 1 + 2 + 3 + b;

let add(a Int, b Int) Int = a + b;

let calc(x Int) Int = add(x, x) + x + 1;

let id(T Any)(id T) T = id;

let id2(T Any)(id T) = id;

let newline Int = 1 + 1 +
                    1 + 1 + 1;

let semicolon() Void = {
    let a = 1;
    let b = 2;
}

let call_id() Void = {
    let a = id(Int)(1);
    let b = id(Float)(1.0);
}

let blockExpr1 Int = {
    let a = 0;
    let b = 0;
    a + b
}

let blockExpr2() Int = {
    let a = 0;
    let b = 0;
    a + b
};

let blockExpr3 Void = {}

let compareExpr Void = {
    let a = 1;
    let b = 1;
    let c = a == b;
    let d = a > b;
    let e = a != b;
    let f = a < b;
}

let logicExpr Void = {
    let a = true & true;
    let b = true | false;
}

let ifExpr1 Int = if true then 0 else if true then 0 else 0;

let ifExpr2 Int = if true then { 0 } else { 1 }

let ifExpr3 Void = if true then {} else {}

let ifExpr4 Void = {
    if true do {}
    if true then {
    } else if true do {
        1
    }
}

let assignment Void = {
    let mut a = 0;
    a = 1;
    a = 2
}

let whileStat() Void = {
    let mut i = 0;
    while i <= 10 do {
        i = i + 1;
    }
}

let localFunction() Int = {
    let f() = 123;
    let a = f();
    a
}

type Integer(v Int);

type Point(x Int, y Int) with {
    copy() Point = Point(x, y);
    plus(other Point) Point = Point(x + other.x, y + other.y);
}

let somePoint Point = Point(1, 2);

let memberAccess(a Point, b Point) Point = Point(a.x+b.x, a.y+b.y).copy();

type Color = { Red(), Green(), Blue() }

let red Color = Red();

type IntList = {
    defaultValue() Int = 0;
    value() Int;
}

type Nil(): IntList with {
    value() Int = 0;
}

type Cons(head Int, tail IntList): IntList with {
    value() Int = head;
}

let makeIntList() IntList = {
    let li IntList = Cons(1, Cons(2, Cons(3, Nil())));
    li.value();
    li
}

let sumIntList(li IntList) Int =
    if li is v Cons then {
        v.head + sumIntList(v.tail)
    } else {
        0
    }

let typePattern() Void = {
    let li IntList = Cons(1, Cons(2, Cons(3, Nil())));
    let i =
        if li is v Cons then {
            v.head
        } else {
            0
        }
}

let typePattern2() Void = {
    let li IntList = Cons(1, Cons(2, Cons(3, Nil())));
    if li is v Cons then {
        v.head
    } else {
        0
    };
}

let identifierPattern() Int = {
    let a = 1;
    if a is a then 123 else 123
}

let identifierPattern2() Void = {
    let a = 1;
    if a is a then {
        localFunction()
    } else if b is a then {
        localFunction()
    } else {
        localFunction()
    };
}

let wildcardPattern() Int = {
    let a = 1;
    if a is _ then 123 else 123
}

let wildcardPattern2() Void = {
    let a = 1;
    if a is _ then {
        localFunction()
    } else if a is _ then {
        localFunction()
    } else {
        localFunction()
    };
}

let literalPattern() Int = {
    let a = 1;
    if a is 1 then 123 else 123
}

let literalPattern2() Void = {
    let a = 1;
    if a is 1 then {
        localFunction()
    } else if a is 1 then {
        localFunction()
    } else {
        localFunction()
    };
}

let ifDoLogical() Void = {
    if true and true and true do {
        localFunction()
    }
    if true or true or true do {
        localFunction()
    }
    if true and true and true or true do {
        localFunction()
    }
    let a = 1;
    if a is 1 and a is 1 do {
        localFunction()
    }
    if a is 1 and a is 1 and (true or true and true) do {
        localFunction()
    }
    if (true or false) and a is 1 do {}
}

let ifThenElseLogical() Void = {
    if true and true and true then {
        localFunction()
    } else { localFunction() }
    if true or true or true then {
        localFunction()
    } else { localFunction() }
    if true and true and true or true then {
        localFunction()
    } else { localFunction() }
    let a = 1;
    if a is 1 and a is 1 then {
        localFunction()
    } else { localFunction() };
}

let whileDoLogical() Void = {
    while true and true and true do {
        localFunction()
    }
    while true or true or true do {
        localFunction()
    }
    while true and true and true or true do {
        localFunction()
    }
    let a = 1;
    while a is 1 and a is 1 do {
        localFunction()
    }
}

let genericsFunction(T)(a T) T = {
    let b T = a;
    b;
    a
}

let callGenericsFunction() Int = genericsFunction(Int)(1);

type Pair(T1, T2)(first T1, second T2);

let genericsRecord() Void = {
    let a Pair(Int, Int) = Pair(Int, Int)(1, 2);
    a.first + a.second;
}

type Option(T);
type Some(T)(value T): Option(T);
type None(T)(): Option(T);

let genericsEnum() Int = {
    let x Option(Int) = Some(Int)(123);
    let y Option(Int) = Some(Int)(123);
    if x is v Some(Int) then v.value else 0
}

type GenericsInterface(T1, T2);
type ImplementType(T)(): GenericsInterface(T, Int);
type ImplementType2(T, R)(): GenericsInterface(R, T);

let genericsSubtyping() Void = {
    let i GenericsInterface(Int, Int) = ImplementType(Int)();
    let j GenericsInterface(Int, Float) = ImplementType2(Float, Int)();
}

let genericsSubtyping2(E)() Void = {
    let i GenericsInterface(E, E) = ImplementType2(E, E)();
}

let array() Void = {
    let mut a = emptyArray(Int)();
    a = newArray(Int)(5, 5);
    let b = a.get(2);
    a.set(2, b);
}

type I1;
type I2 = {
    f() Void;
    f2(a Int) Int;
}

type C1(): I2 with {
    f() Void = {};
    f2(a Int) Int = a;
}

let interfaceInstance() I2 = {
    let i I2 = C1();
    let f(x I2) = 123;
    f(C1());
    C1()
}

let interfaceConstraint(T I2)(v T) Void = {
    v.f();
    let x = v.f2(1);
    let i I2 = v;
}

let instanceConstraint() Void = {
    interfaceConstraint(C1)(C1());
}

type Eq(T) = {
    eq(other T) Bool;
}

type EqFoo(): Eq(EqFoo) with {
    eq(other EqFoo) Bool = true;
}

type EqBar(): I1;

given EqBar: Eq(EqBar) with {
    eq(other EqBar) Bool = true;
}

type EqPair(T1 Eq(T1), T2 Eq(T2))(first T1, second T2): Eq(EqPair(T1, T2)) with {
    eq(other EqPair(T1, T2)) Bool = first.eq(other.first) & second.eq(other.second);
}

type EqPair2(T1, T2)(first T1, second T2);

given EqPair2(T1 Eq(T1), T2 Eq(T2)): Eq(EqPair2(T1, T2)) with {
    eq(other EqPair2(T1, T2)) Bool = first.eq(other.first) & second.eq(other.second);
}

let complicatedGenericsType(T Eq(T))(a T, b T) Bool = a.eq(b);

let complicatedGenericsType2() Void = {
    complicatedGenericsType(EqFoo)(EqFoo(), EqFoo());
    let a = EqPair(EqFoo, EqFoo)(EqFoo(), EqFoo());
    complicatedGenericsType(EqPair(EqFoo, EqFoo))(a, a);
    let b Eq(EqPair(EqFoo, EqFoo)) = a;
    let c Eq(EqPair2(EqBar, EqBar)) = EqPair2(EqBar, EqBar)(EqBar(), EqBar());
}

let functionType(x Int, f (Int)-> Int) Int = f(x);
let lambdaExpression() Int = functionType(1, (a Int) => a);
let lambdaExpression2() Int = functionType(1, (b Int) Int => b);

