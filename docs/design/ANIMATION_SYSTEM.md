# Kaiteyo (書いてよ) — Animation System

## Animation Philosophy

Animations in Kaiteyo serve a purpose: they guide attention, provide feedback, and make the interface feel alive. Every animation should be:
- **Subtle** — Never distracting or gratuitous
- **Fast** — Complete within 150-300ms for micro-interactions
- **Natural** — Use spring physics for organic feel
- **Consistent** — Same animation for same interaction everywhere

## Animation Presets

### No Animation
- All transitions instant (0ms)
- For accessibility (reduced motion)
- No spring physics

### Minimal
- Duration: 100ms
- Easing: linear
- No spring physics
- Only essential transitions

### Standard (Default)
- Duration: 200ms
- Easing: ease-in-out
- Spring damping: 0.7
- Spring stiffness: 300

### Smooth
- Duration: 300ms
- Easing: ease-out
- Spring damping: 0.5
- Spring stiffness: 200

### Bouncy
- Duration: 400ms
- Easing: spring
- Spring damping: 0.3
- Spring stiffness: 150

## Animation Types

### Hover Animations
| Property | Start | End | Duration | Easing |
|----------|-------|-----|----------|--------|
| Scale | 1.0 | 1.02-1.15 | 150ms | spring(damping=0.5) |
| Background | transparent | glow color | 200ms | tween(ease-out) |
| Text Color | default | hover color | 200ms | tween(ease-out) |
| Shadow | 0dp | 4dp | 200ms | tween(ease-out) |

### Press Animations
| Property | Start | End | Duration | Easing |
|----------|-------|-----|----------|--------|
| Scale | 1.0 | 0.97 | 80ms | tween(ease-in) |
| Background | normal | pressed | 80ms | tween(ease-in) |

### Focus Animations
| Property | Start | End | Duration | Easing |
|----------|-------|-----|----------|--------|
| Glow Ring | invisible | visible | 200ms | tween(ease-out) |
| Border | default | accent | 200ms | tween(ease-out) |

### Expand/Collapse Animations
| Property | Start | End | Duration | Easing |
|----------|-------|-----|----------|--------|
| Width | 56dp | 280dp | 300ms | spring(damping=0.6) |
| Opacity | 0 | 1 | 200ms | tween(ease-out) |
| Content Fade | 0 | 1 | 150ms (delayed) | tween(ease-out) |

### Navigation Transitions
| Type | Duration | Easing |
|------|----------|--------|
| Page slide | 300ms | spring(damping=0.7) |
| Fade through | 250ms | tween(ease-in-out) |
| Shared element | 350ms | spring(damping=0.6) |

### Theme Switching
| Property | Duration | Easing |
|----------|----------|--------|
| Color transition | 400ms | tween(ease-out) |
| Background crossfade | 500ms | tween(ease-in-out) |
| Content fade | 200ms | tween(ease-out) |

### Dialog/Modal Animations
| Property | Start | End | Duration | Easing |
|----------|-------|-----|----------|--------|
| Scale | 0.9 | 1.0 | 250ms | spring(damping=0.6) |
| Opacity | 0 | 1 | 200ms | tween(ease-out) |
| Backdrop | 0 | 0.6 | 200ms | tween(ease-out) |

### Sidebar Animations
| Property | Duration | Easing |
|----------|----------|--------|
| Dock transition | 300ms | spring(damping=0.5) |
| Collapse/expand | 300ms | spring(damping=0.6) |
| Auto-hide reveal | 200ms | tween(ease-out) |
| Snap to position | 250ms | spring(damping=0.7) |

## Implementation Patterns

### Using animateXAsState
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isHovered) 1.05f else 1f,
    animationSpec = spring(
        dampingRatio = 0.5f,
        stiffness = 300f
    ),
    label = "elementScale"
)
```

### Using Animatable
```kotlin
val scale = remember { Animatable(1f) }
LaunchedEffect(isHovered) {
    scale.animateTo(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.5f)
    )
}
```

### Using Transition
```kotlin
val transition = updateTransition(targetState = isHovered, label = "hover")
val bgColor by transition.animateColor(label = "bg") { hovered ->
    if (hovered) glowColor else Color.Transparent
}
```

## Performance Guidelines

1. **Use `remember`** for animation states to avoid recomposition
2. **Prefer `animateXAsState`** over `Animatable` for simple values
3. **Use `LaunchedEffect`** for one-shot animations
4. **Avoid animating layout properties** (use `graphicsLayer` instead)
5. **Keep animated composables small** — don't re-animate entire trees
6. **Use `@Stable` annotations** on animation state holders
7. **Target 60 FPS** — if dropping frames, simplify animations
8. **Use `derivedStateOf`** to avoid unnecessary animation recalculations

## Reduced Motion

When the user's system prefers reduced motion:
- Disable all spring animations (use tween with 0ms)
- Skip hover scale animations
- Use instant transitions for navigation
- Fade instead of slide for page transitions
- Disable parallax and decorative motion

```kotlin
val motionDuration = if (isReducedMotion) 0 else 200
